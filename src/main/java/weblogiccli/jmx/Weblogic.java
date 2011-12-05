package weblogiccli.jmx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weblogic.deploy.api.spi.DeploymentOptions;
import weblogic.deploy.api.spi.WebLogicDeploymentManager;
import weblogic.deploy.api.spi.WebLogicTargetModuleID;
import weblogic.deploy.api.tools.SessionHelper;
import weblogic.management.configuration.AppDeploymentMBean;
import weblogic.management.jmx.MBeanServerInvocationHandler;
import weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean;
import weblogic.management.runtime.AppRuntimeStateRuntimeMBean;
import weblogic.management.runtime.ServerLifeCycleRuntimeMBean;
import weblogiccli.cmd.exception.ServerIsNotRunningException;
import weblogiccli.cmd.exception.UnknownTargetException;
import weblogiccli.conf.Environment;
import weblogiccli.utils.Colorize;

public class Weblogic {

	static Logger LOG = LoggerFactory.getLogger(Weblogic.class);

	private String protocol;
	private String host;
	private int port;
	private String username;
	private String password;
	
    public Weblogic(Environment e) {
        this.protocol = "t3";
        this.host = e.getHost();
        this.port = e.getPort();
        this.username = e.getUsername();
        this.password = e.getPassword();
    }

    private String getAdminurl() {
        return protocol + "://" + host + ":" + port;
    }
    
    // singleton
    private static DomainRuntimeServiceMBean _domainRuntimeServiceMBean;
    private DomainRuntimeServiceMBean getDomainRuntimeServiceMBean() throws WeblogicException {
        if (_domainRuntimeServiceMBean == null) {
            
            // Connexion JMX
            MBeanServerConnection connection;
            try {
                JMXServiceURL url = new JMXServiceURL(protocol, host, port, "/jndi/weblogic.management.mbeanservers.domainruntime");
                Map<String, Object> h = new HashMap<String, Object>();
                h.put(Context.SECURITY_PRINCIPAL, username);
                h.put(Context.SECURITY_CREDENTIALS, password);
                h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
                JMXConnector connector = JMX.connectWithTimeout(url, h, 5);
                connection = connector.getMBeanServerConnection();
            } catch (Exception e) {
                throw new WeblogicException("Impossible de se connecter au serveur Weblogic : " + getAdminurl() + " avec l'utilisateur " + username, e);
            }
            
            // Récupération du mbean racine : DomainRuntimeServiceMBean 
            try {
                ObjectName objectName = new ObjectName(DomainRuntimeServiceMBean.OBJECT_NAME);
                _domainRuntimeServiceMBean = (DomainRuntimeServiceMBean) MBeanServerInvocationHandler.newProxyInstance(connection, objectName, DomainRuntimeServiceMBean.class, false);
            } catch (MalformedObjectNameException e) {
                throw new RuntimeException(e);
            }
            
        }
        return _domainRuntimeServiceMBean;
    }
    
    private WebLogicDeploymentManager getDeploymentManager() throws DeploymentManagerCreationException  {
        return SessionHelper.getRemoteDeploymentManager(protocol, host, String.valueOf(port), username, password);
    }
	
	// Commands
	
	/**
	 * Return the list of applications deployed to the domain.  
	 */
	public List<String> listapps() throws Exception {
        List<String> result = new ArrayList<String>();
        AppDeploymentMBean[] appDeployments = getDomainRuntimeServiceMBean().getDomainConfiguration().getAppDeployments();
        for (AppDeploymentMBean a : appDeployments) {
            result.add(appDeploymentMBeanToString(a));
        }
        Collections.sort(result);
        return result;
	}

    public void deploy(String name, File war, String target) throws Exception {
        deploy(name, war, target, false);
    }
    
    public void deploy(String name, File war, String target, boolean dryrun) throws Exception {
        String version = retrieveWeblogicApplicationVersion(war);
        LOG.info("Installation de {} :", appName(name, version));
        
        if (dryrun) {
        	return;
        }
        
        WebLogicDeploymentManager deployer = getDeploymentManager();
        
        try {
            // Options
            DeploymentOptions options = new DeploymentOptions();
            options.setName(name);
            options.setTimeout(1000*120); // 2 minutes max, sinon on considère que le déploiement a échoué
            
            Target t = getTarget(target, deployer);
            WebLogicTargetModuleID targetModuleID = deployer.createTargetModuleID(name, ModuleType.WAR, t);
        
            wait(deployer.deploy(new TargetModuleID[] { targetModuleID }, war, null, options));
            
        } finally {
            deployer.release();
        }
    }

    /**
     * Réinstalle une application déjà uploadée.
     */
    public void restore(AppDeploymentMBean appDeployment, String target) throws Exception {
        LOG.info("Restauration de {} :", appDeployment.getAbsoluteSourcePath());

        // Options
        DeploymentOptions options = new DeploymentOptions();
        options.setName(appDeployment.getApplicationName());
        options.setRemote(true);
        options.setTimeout(2*60*1000); // 2 minutes max, sinon on considère que le déploiement a échoué
        
        WebLogicDeploymentManager deployer = getDeploymentManager();

        try {
            Target t = getTarget(target, deployer);
            WebLogicTargetModuleID targetModuleID = deployer.createTargetModuleID(appDeployment.getApplicationName(), ModuleType.WAR, t);

            File war = new File(appDeployment.getAbsoluteSourcePath());
            wait(deployer.deploy(new TargetModuleID[] { targetModuleID }, war, null, options));

        } finally {
            deployer.release();
        }
    }
    
    public void undeploy(String name, String version) throws Exception {
        undeploy(appName(name, version), false);
    }
	
	public void undeploy(String name, boolean dryryn) throws Exception {
        LOG.info("Désinstallation de {} :", name);
        if (dryryn) {
        	return;
        }
        
        // Options
        DeploymentOptions options = new DeploymentOptions();
        options.setTimeout(60*1000); // 1 minute max
        
		WebLogicDeploymentManager deployer = getDeploymentManager();
		
		try {
            TargetModuleID[] tmids = findTargetModuleIDs(name);
	        wait(deployer.undeploy(tmids));
		} finally {
			deployer.release();
		}
	}
	
	// Utilities

    private Target getTarget(String target, WebLogicDeploymentManager deployer) throws UnknownTargetException {
        Target t = deployer.getTarget(target);
        if (t == null) {
            List<String> targets = new ArrayList<String>();
            for (Target trg : deployer.getTargets()) {
                targets.add(trg.getName());
            }
            String message = "La cible \"" + target + "\" est inconnue sur cet environnement (" + StringUtils.join(targets, ", ") + ").";
            throw new UnknownTargetException(message);
        }
        return t;
    }
    
    /**
     * Recherche les {@link TargetModuleID} d'une application.
     */
    private TargetModuleID[] findTargetModuleIDs(String name) throws Exception {
        WebLogicDeploymentManager deployer = getDeploymentManager();
        String errorMsg = "L'application \"" + name + "\" n'existe pas sur cette instance.";
        try {
            TargetModuleID[] tmids = deployer.getAvailableModules(ModuleType.WAR, deployer.getTargets());
            if (tmids == null) {
                throw new RuntimeException(errorMsg);
            }
            TargetModuleID[] filteredTmids = deployer.filter(tmids, name, null, null);
            if (filteredTmids == null) {
                throw new RuntimeException(errorMsg);
            }
            return filteredTmids;
        } finally {
            deployer.release();
        }
    }
	
	/**
	 * Retourne la version du fichier WAR ("Weblogic-Application-Version" dans le fichier MANIFEST.MF), si
	 * elle existe. Sinon, retourne <code>null</code>.
	 */
    public String retrieveWeblogicApplicationVersion(File war) throws IOException {
        JarFile jar = new JarFile(war);
        Manifest manifest = jar.getManifest();
        if (manifest != null) {
            return manifest.getMainAttributes().getValue("Weblogic-Application-Version");
        } else {
            return null;
        }
    }

    /**
     * Chercher les applications, à partir de leur nom.
     */
    public Set<AppDeploymentMBean> findApplications(String name) throws Exception {
        AppDeploymentMBean[] appDeploymentMBeans = getDomainRuntimeServiceMBean().getDomainConfiguration().getAppDeployments();
        
        Set<AppDeploymentMBean> result = new HashSet<AppDeploymentMBean>();
        for (AppDeploymentMBean a : appDeploymentMBeans) {
            if (a.getApplicationName().equals(name)) {
                result.add(a);
            } else {
                String[] split = a.getApplicationName().split("#");
                if (split.length == 2 && a.getApplicationName().equals(split[0])) {
                    result.add(a);
                }
            }
        }
        return result;
    }

    /**
     * Chercher les applications dans l'état STATE_ACTIVE, à partir de leur nom.
     */
	public Set<AppDeploymentMBean> findActiveApplications(String name) throws Exception {
        Set<AppDeploymentMBean> apps = findApplications(name);
        
        // Versions actives
        Set<AppDeploymentMBean> activeApps = new HashSet<AppDeploymentMBean>();
        for (AppDeploymentMBean a : apps) {
            if (appState(a).equals("STATE_ACTIVE")) {
                activeApps.add(a);
            }
        }
        return activeApps;
	}

	/**
	 * Vérifie que les serveurs du domaine sont tous à RUNNING. Affiche les états en couleur.
	 * <p>
	 * Lève une exception {@link ServerIsNotRunningException} si tous les serveurs ne sont 
	 * pas à RUNNING.
	 * 
	 * @throws Exception
	 */
    public void checkServersAreRunning() throws Exception {
        LOG.info("Vérification des serveurs :");

        // Compte les serveurs définis dans l'environnement
        int configServersCount = getDomainRuntimeServiceMBean().getDomainConfiguration().getServers().length;
        
        ServerLifeCycleRuntimeMBean[] servers = getDomainRuntimeServiceMBean().getDomainRuntime().getServerLifeCycleRuntimes();
        int runtimeServersCount = servers.length;
        
        // A tester, pas sûr que ça marche vraiment...
        if (runtimeServersCount != configServersCount) {
            throw new ServerIsNotRunningException("Seuls " + runtimeServersCount + " sont démarrés, alors qu'il devrait il y en avoir " + configServersCount + ".");
        }
        
        Map<String, String> serversStates = new HashMap<String, String>();
        int maxLength = 0;
        String error = null;
        for (ServerLifeCycleRuntimeMBean server : servers) {
            String state = server.getState();
            serversStates.put(server.getName(), state);
            if (server.getName().length() > maxLength) {
                maxLength = server.getName().length();
            }
            if (!state.equals("RUNNING")) {
                error = "Le serveur " + server.getName() + " n'est pas disponible!";
            }
        }
        for (Entry<String, String> e : serversStates.entrySet()) {
            String state = e.getValue();
            if (state.equals("RUNNING")) {
                state = Colorize.green(state);
            } else {
                state = Colorize.red(state);
            }
            LOG.info(StringUtils.leftPad(e.getKey(), maxLength+2) + " - " + state);
        }
        if (error != null) {
            throw new ServerIsNotRunningException(error);
            
        }
    }
	
    /**
     * Retourne le nom de l'application au format des noms de déploiement Weblogic : nom[#version].
     */
    public String appName(String name, String version) {
        String v = "";
        if (version != null) {
            v = "#" + version;
        }
        return name + v;
    }

    /**
     * Retourne le nom de l'application au format : nom [version=version] (état).
     */
    public String appDeploymentMBeanToString(AppDeploymentMBean a) throws Exception {
        String version = "";
        if (a.getVersionIdentifier() == null) {
            version = "";
        } else {
            version = " [version=" + a.getVersionIdentifier() + "]";
        }
        return a.getApplicationName() + version + " (" + appState(a) + ")";
    }
    
    /**
     * Retourne l'état d'une application
     */
    // TODO : gérer toutes les cibles...
    public String appState(AppDeploymentMBean a) throws WeblogicException {
        AppRuntimeStateRuntimeMBean appRuntimeState = getDomainRuntimeServiceMBean().getDomainRuntime().getAppRuntimeStateRuntime();
        if (a.getTargets().length == 0) {
            return "???";
        } else {
            return appRuntimeState.getCurrentState(a.getName(), a.getTargets()[0].getName());
        }
    }
    
    /**
     * Attend que le traitement se termine, et affiche la progression.
     */
    public static void wait(ProgressObject progress) throws InterruptedException {
    	long sleepTime = 100;
    	long timeElapsed = 0;
		while (progress.getDeploymentStatus().isRunning()) {
		    System.out.print(".");
            System.out.flush();
            Thread.sleep(sleepTime);
            timeElapsed += sleepTime;
        }
        if (timeElapsed >= sleepTime) {
            System.out.println("");
        }
        DeploymentStatus status = progress.getDeploymentStatus();
        if (status.isFailed()) {
            throw new RuntimeException(status.getMessage());
        }
    }
}

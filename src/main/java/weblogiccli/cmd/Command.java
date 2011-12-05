package weblogiccli.cmd;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weblogiccli.cmd.exception.ArgsException;
import weblogiccli.cmd.exception.EnvironmentException;
import weblogiccli.conf.Environment;

public abstract class Command {

    private static Logger LOG = LoggerFactory.getLogger(Command.class);
    
    protected Map<String, Environment> environments;
	
	public Command() {
        super();
    }
	
	public Command(Map<String, Environment> environments) {
        super();
        this.environments = environments;
    }

    public abstract void printHelp();
	
	public abstract void run(String[] args) throws Exception;
    
    public Environment checkEnvironment(String environment) throws EnvironmentException {
        Environment r = environments.get(environment);
        if (r == null) {
            throw new EnvironmentException("L'environnement \"" + environment + "\" est inconnu!");
        } else {
            return r;
        }
    }
    
    // Args
    
    public String getByPosition(String[] args, int pos) throws ArgsException {
        try {
            String value = args[pos];
            if (value.startsWith("-")) {
                throw new ArgsException();
            } else {
                return value;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArgsException();
        }
    }

    public String getOption(String[] args, String option) throws ArgsException {
        return getOption(args, option, null);
    }
    
    public String getOption(String[] args, String option, String defaultValue) throws ArgsException {
        int i = 0;
        for (String arg : args) {
            if (arg.equals(option)) {
                try {
                    return args[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new ArgsException();
                }
            }
            i++;
        }
        return defaultValue;
    }

    public boolean getFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }

    // Printers
    
    public void printEnvironments() {
        printEnvironments(environments);
    }
    
    public void printEnvironments(Map<String, Environment> environments) {
        LOG.info("Environnements connus :");
        for (String name : environments.keySet()) {
            LOG.info("  " + name);
        }
    }
    
    public File checkWar(String warFilename) {
        File war = new File(warFilename);
        if (!war.exists()) {
            throw new IllegalArgumentException("Le fichier \"" + warFilename + "\" n'existe pas!");
        }
        return war;
    }
}

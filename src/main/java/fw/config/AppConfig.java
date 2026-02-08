package fw.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utilitaire pour charger la configuration depuis app.properties
 */
public class AppConfig {
    
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "app.properties";
    private static boolean isLoaded = false;
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.out.println("Fichier " + CONFIG_FILE + " non trouvé. Utilisation des valeurs par défaut.");
                properties.setProperty("session.user.key", "userSession");
                return;
            }
            properties.load(input);
            isLoaded = true;
            System.out.println("Configuration chargée depuis " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de " + CONFIG_FILE + ": " + e.getMessage());
            // Valeurs par défaut
            properties.setProperty("session.user.key", "userSession");
        }
    }
    

    public static String getSessionUserKey() {
        return properties.getProperty("sessi/**\n" + //
                        " * Implémentation de UserSession pour le projet teste_fw\n" + //
                        " * Gère les rôles de l'utilisateur authentifié\n" + //
                        " */on.user.key", "userSession");
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}

package fw.security;

/**
 * Interface pour gérer les rôles de l'utilisateur en session
 * L'implémentation doit être fournie par le projet utilisant le framework
 */
public interface UserSession {

    /**
     * Vérifie si l'utilisateur a un rôle spécifié
     * 
     * @param role le rôle à vérifier
     * @return true si l'utilisateur a le rôle, false sinon
     */
    boolean hasRole(String role);

}

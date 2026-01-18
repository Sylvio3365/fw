package fw.helper;

import fw.annotation.session.SessionInjection;
import fw.session.Session;
import fw.session.SessionManager;
import fw.session.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class SessionInjectionHelper {

    public static Object[] injectSessionParameters(Method method, HttpServletRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        String sessionId = SessionUtils.createSessionIdFromRequest(request);
        SessionManager sessionManager = SessionManager.getInstance();
        Session session = sessionManager.getSession(sessionId);
        
        if (session == null) {
            session = sessionManager.createSession(sessionId);
        }

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            // Injection de Session
            if (param.isAnnotationPresent(SessionInjection.class)) {
                SessionInjection sessionAnnotation = param.getAnnotation(SessionInjection.class);
                String attributeKey = sessionAnnotation.value();

                if (attributeKey.isEmpty()) {
                    // Injecter toute la session
                    args[i] = session;
                } else {
                    // Injecter un attribut spécifique de la session
                    args[i] = session.getAttribute(attributeKey);
                }
            }
            // Injection de HttpServletRequest (existant)
            else if (param.getType() == HttpServletRequest.class) {
                args[i] = request;
            }
            // Injection de autres paramètres...
            else {
                args[i] = null;
            }
        }

        return args;
    }

    public static boolean hasSessionInjection(Method method) {
        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(SessionInjection.class)) {
                return true;
            }
        }
        return false;
    }
}
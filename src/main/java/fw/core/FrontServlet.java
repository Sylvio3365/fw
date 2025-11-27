package fw.core;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import fw.helper.CMethod;
import fw.helper.Helper;
import fw.util.ModelView;

public class FrontServlet extends HttpServlet {

    private Helper h;

    @Override
    public void init() throws ServletException {
        this.h = new Helper();
        Map<String, CMethod> urlMappings = h.scan(h.getAllPackages());
        ServletContext context = getServletContext();
        context.setAttribute("urlMappings", urlMappings);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (ressourceExist(request)) {
            customServe(request, response);
        } else {
            try {
                defaultServe(request, response);
            } catch (IllegalArgumentException
                    | SecurityException | IOException e) {
                throw new ServletException(e);
            }
        }
    }

    private boolean ressourceExist(HttpServletRequest req) throws MalformedURLException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        URL ressouce = getServletContext().getResource(path);
        return ressouce != null;
    }

    private void customServe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request, response);
    }

    private void defaultServe(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/html;charset=UTF-8");

        try {
            String url = this.h.getUrlAfterContext(request);
            ServletContext context = getServletContext();
            Map<String, CMethod> urlMappings = (Map<String, CMethod>) context.getAttribute("urlMappings");

            if (!this.h.findByUrl(urlMappings, url)) {
                sendNotFound(response, url);
                return;
            }

            String originalUrl = h.getOriginalUrl(urlMappings, url);
            CMethod cm = h.getUrlInMapping(urlMappings, url);

            if (cm == null) {
                throw new ServletException("Aucune classe méthode trouvée pour l'URL: " + url);
            }
            // Si l'URL correspond exactement (sans variables)
            if (url.equals(originalUrl)) {
                processExactMatch(request, response, url, originalUrl, cm);
            }
            // Si l'URL contient des variables de chemin
            else {
                processPatternMatch(request, response, url, originalUrl, cm, urlMappings);
            }
        } catch (Exception e) {
            handleError(response, e);
        }
    }

    // ==================== MÉTHODES AUXILIAIRES ====================

    private void processExactMatch(HttpServletRequest request, HttpServletResponse response,
            String url, String originalUrl, CMethod cm)
            throws Exception {

        Class<?> cls = cm.getClazz();
        Method method = cm.getMethod();
        Object[] arguments = h.getArgumentsWithValue(method, request);
        Object instance = cls.getDeclaredConstructor().newInstance();

        Class<?> returnType = method.getReturnType();

        if (returnType.equals(String.class)) {
            Object result = method.invoke(instance, arguments);
            sendStringResponse(response, url, originalUrl, result);
        } else if (returnType.equals(ModelView.class)) {
            ModelView result = (ModelView) method.invoke(instance, arguments);
            forwardToView(request, response, result);
        } else {
            sendUnsupportedTypeResponse(response, url);
        }
    }

    private void processPatternMatch(HttpServletRequest request, HttpServletResponse response,
            String url, String originalUrl, CMethod cm,
            Map<String, CMethod> urlMappings)
            throws Exception {

        Class<?> cls = cm.getClazz();
        Method method = cm.getMethod();

        Map<String, String> pathVariables = h.extractPathVariables(originalUrl, url);
        Object[] arguments = h.getArgumentsWithValue(method, pathVariables);
        Object instance = cls.getDeclaredConstructor().newInstance();
        Class<?> returnType = method.getReturnType();

        if (returnType.equals(String.class)) {
            Object result = method.invoke(instance, arguments);
            sendStringResponse(response, url, originalUrl, result);
        } else if (returnType.equals(ModelView.class)) {
            ModelView result = (ModelView) method.invoke(instance, arguments);
            forwardToView(request, response, result);
        } else {
            sendUnsupportedTypeResponse(response, url);
        }
    }

    private void sendStringResponse(HttpServletResponse response, String url,
            String originalUrl, Object result) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>URL trouvé</h1>");
            out.println("<p>URL : " + url + "</p>");
            out.println("<p>Pattern : " + originalUrl + "</p>");
            out.println("<p>Résultat : " + result + "</p>");
            out.println("</body></html>");
        }
    }

    private void forwardToView(HttpServletRequest request, HttpServletResponse response, ModelView modelView)
            throws ServletException, IOException {

        String view = modelView.getView();
        Map<String, Object> data = modelView.getData();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(view);
        dispatcher.forward(request, response);
    }

    private void sendUnsupportedTypeResponse(HttpServletResponse response, String url) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>URL trouvé</h1>");
            out.println("<p>URL : " + url + " - type de retour non supporté</p>");
            out.println("</body></html>");
        }
    }

    private void sendNotFound(HttpServletResponse response, String url) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>404 - Not Found</h1>");
            out.println("<p>URL non trouvée : " + url + "</p>");
            out.println("</body></html>");
        }
    }

    private void handleError(HttpServletResponse response, Exception e) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>Erreur</title>");
            out.println("<style>");
            out.println(".error { color: red; background-color: #ffe6e6; padding: 10px; border: 1px solid red; }");
            out.println("</style>");
            out.println("</head><body>");
            out.println("<h1>Erreur lors du traitement</h1>");
            out.println("<div class='error'>");
            out.println("<strong>Message :</strong> " + e.getMessage() + "<br>");
            out.println("<strong>Type :</strong> " + e.getClass().getSimpleName());
            out.println("</div>");
            out.println("</body></html>");
        }
        e.printStackTrace(); // Pour les logs du serveur
    }

}

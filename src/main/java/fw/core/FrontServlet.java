package fw.core;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException | IOException e) {
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
            throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ServletException {
        response.setContentType("text/html;charset=UTF-8");
        String url = this.h.getUrlAfterContext(request);
        ServletContext context = getServletContext();
        Map<String, CMethod> urlMappings = (Map<String, CMethod>) context.getAttribute("urlMappings");
        if (this.h.findByUrl(urlMappings, url)) {
            CMethod cm = h.getUrlInMapping(url, urlMappings);
            if (cm == null) {
                throw new ServletException("Aucune méthode trouvée pour l'URL: " + url);
            }

            Class<?> cls = cm.getClazz();
            Method method = cm.getMethod();
            
            List<String> paramNames = h.getParametersName(method);

            if (method.getReturnType().equals(String.class)) {
                Object instance = cls.getDeclaredConstructor().newInstance();
    
                Object[] arguments = { arg1 };
                Object result = method.invoke(instance, arguments);
                String teste = h.testeAffiche(method);
                try (PrintWriter out = response.getWriter()) {
                    out.println("<html><head><title>FrontServlet</title></head><body>");
                    out.println("<h1>URL trouvé</h1>");
                    out.println("<p> URL : " + url + "</p>");
                    out.println("<p>" + urlMappings.get(url) + "</p>");
                    out.println("<p>" + teste + "</p>");
                    out.println("<p> Resulat : " + result + "</p>");
                }
            }
            if (method.getReturnType().equals(ModelView.class)) {
                Object instance = cls.getDeclaredConstructor().newInstance();
                ModelView result = (ModelView) method.invoke(instance);
                String view = result.getView();
                Map<String, Object> data = result.getData();
                List<String> keys = new ArrayList<>(data.keySet());
                for (int i = 0; i < data.size(); i++) {
                    String key = keys.get(i);
                    request.setAttribute(key, data.get(key));
                }
                RequestDispatcher rd = request.getRequestDispatcher(view);
                rd.forward(request, response);
            } else {
                try (PrintWriter out = response.getWriter()) {
                    out.println("<html><head><title>FrontServlet</title></head><body>");
                    out.println("<h1>URL trouvé</h1>");
                    out.println("<p> URL : " + url + "non supporte </p>");
                }
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>FrontServlet</title></head><body>");
                out.println("<h1>404 - Not found</h1>");
                out.println("</body></html>");
            }
        }
    }

}

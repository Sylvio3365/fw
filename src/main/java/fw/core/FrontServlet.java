package fw.core;

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
import java.util.Map;
import fw.helper.CMethod;
import fw.helper.Helper;

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
            InvocationTargetException, NoSuchMethodException, SecurityException {
        response.setContentType("text/html;charset=UTF-8");
        String url = this.h.getUrlAfterContext(request);
        ServletContext context = getServletContext();
        Map<String, CMethod> urlMappings = (Map<String, CMethod>) context.getAttribute("urlMappings");
        if (this.h.findByUrl(urlMappings, url)) {
            CMethod cm = urlMappings.get(url);
            Class<?> cls = cm.getClazz();
            Method method = cm.getMethod();

            if (method.getReturnType().equals(String.class)) {
                Object instance = cls.getDeclaredConstructor().newInstance();
                Object result = method.invoke(instance);
                try (PrintWriter out = response.getWriter()) {
                    out.println("<html><head><title>FrontServlet</title></head><body>");
                    out.println("<h1>URL trouvé</h1>");
                    out.println("<p> URL : " + url + "</p>");
                    out.println("<p>" + urlMappings.get(url) + "</p>");
                    out.println("<p> Resulat : " + result + "</p>");
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

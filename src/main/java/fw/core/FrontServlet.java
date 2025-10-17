package fw.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (ressourceExist(request)) {
            customServe(request, response);
        } else {
            defaultServe(request, response);
        }
    }

    private boolean ressourceExist(HttpServletRequest req) throws MalformedURLException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        URL ressouce = getServletContext().getResource(path);
        return ressouce != null;
    }

    private void customServe(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request, response);
    }

    private void defaultServe(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html><head><title>FrontServlet</title></head><body>");
            out.println("<h1>FrontServlet - Page par défaut</h1>");
            out.println("<p>Méthode HTTP : " + request.getMethod() + "</p>");
            out.println("<p>URL : " + request.getRequestURL() + "</p>");
            out.println("</body></html>");
        }
    }
}

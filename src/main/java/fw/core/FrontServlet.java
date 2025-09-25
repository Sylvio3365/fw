package fw.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Ici tu peux gérer toutes les méthodes HTTP (GET, POST, etc.)

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head><title>FrontServlet</title></head>");
            out.println("<body>");
            out.println("<h1>Bonjour depuis FrontServlet !</h1>");
            out.println("<p>Méthode HTTP utilisée : " + request.getMethod() + "</p>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}

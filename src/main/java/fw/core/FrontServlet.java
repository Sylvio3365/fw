package fw.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fw.helper.Helper;
import fw.helper.UrlCM;

public class FrontServlet extends HttpServlet {

    private Helper h;
    private List<UrlCM> urlMappings;

    @Override
    public void init() throws ServletException {
        this.h = new Helper();
        
        List<String> listePackage = new ArrayList<>();
        listePackage.add("controller");

        this.urlMappings = this.h.getUrl(listePackage);
    }

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

    private void customServe(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request, response);
    }

    private void defaultServe(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        UrlCM urlCm = this.h.findByUrl(this.urlMappings, this.getUrlAfterContext(request));
        if (urlCm != null) {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>FrontServlet</title></head><body>");
                out.println("<h1>URL trouvé</h1>");
                out.println("<p>URL : " + urlCm.getUrl() + "</p>");
                out.println("<p>Class : " + urlCm.getCm().getClazz().getCanonicalName() + "</p>");
                out.println("<p>Method : " + urlCm.getCm().getMethod().getName() + "</p>");
                out.println("</body></html>");
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><head><title>FrontServlet</title></head><body>");
                out.println("<h1>404 - Not found</h1>");
                out.println("</body></html>");
            }
        }
    }

    private String getUrlAfterContext(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        return requestURI.substring(contextPath.length());
    }
}

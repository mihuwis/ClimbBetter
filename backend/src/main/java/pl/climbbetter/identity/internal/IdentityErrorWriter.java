package pl.climbbetter.identity.internal;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

final class IdentityErrorWriter {
    private IdentityErrorWriter() { }

    static void write(HttpServletResponse response, IdentityAccessException error) throws IOException {
        response.setStatus(error.status());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        // Only module-owned constants enter this response, never a token or provider exception.
        response.getWriter().write("{\"status\":" + error.status() + ",\"code\":\"" + error.code()
                + "\",\"detail\":\"" + error.getMessage() + "\"}");
    }
}

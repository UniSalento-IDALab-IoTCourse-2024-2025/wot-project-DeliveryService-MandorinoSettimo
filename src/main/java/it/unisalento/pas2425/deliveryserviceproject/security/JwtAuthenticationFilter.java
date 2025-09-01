package it.unisalento.pas2425.deliveryserviceproject.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtilities jwtUtilities ;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = authorizationHeader.substring(7);

        // 1. Verifica che non sia scaduto
        Date expirationDate = jwtUtilities.extractExpiration(jwt);
        if (expirationDate.before(new Date())) {
            System.out.println("Token scaduto.");
            chain.doFilter(request, response);
            return;
        }

        // 2. Estrai lo userId dal token
        String userId = jwtUtilities.extractClaim(jwt, claims -> claims.get("userId", String.class));
        if (userId == null) {
            System.out.println("Token non contiene userId.");
            chain.doFilter(request, response);
            return;
        }

        // 3. Se il contesto non ha già autenticazione, autentica l'utente
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String role = jwtUtilities.extractRole(jwt);

            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities); // 👈 autorizzazioni reali

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);


            System.out.println("Utente autenticato con userId: " + userId + ", ruolo: " + role);
        }

        chain.doFilter(request, response);
    }
}

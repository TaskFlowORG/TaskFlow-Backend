package br.demo.backend.security.controller;


import br.demo.backend.security.model.UserLogin;
import br.demo.backend.security.utils.CookieUtil;
import br.demo.backend.security.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor


public class AuthenticateController {

    private AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil = new JwtUtil();
    private final CookieUtil cookieUtil = new CookieUtil();
    @PostMapping("/login")
    public ResponseEntity<String> authenticate(@RequestBody UserLogin userLogin, HttpServletRequest request, HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userLogin.getUsername(), userLogin.getPassword());
            Authentication authentication = authenticationManager.authenticate(token);
            UserDetails user = (UserDetails) authentication.getPrincipal();// Get the user from the authentication object
            Cookie cookie = cookieUtil.gerarCookieJwt(user);// Create a cookie with the JWT
            response.addCookie(cookie);// Add the cookie to the response

            return ResponseEntity.ok("User authenticated");

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

    }
}
package br.demo.backend.security.service;

import br.demo.backend.model.Code;
import br.demo.backend.model.User;
import br.demo.backend.repository.UserRepository;
import br.demo.backend.security.utils.CookieUtil;
import br.demo.backend.service.EmailService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.coyote.Request;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> userOptional = userRepository.findByUserDetailsEntity_Username(username);
        if (userOptional.isPresent()) {
            User userGet = userOptional.get();
            userGet.getUserDetailsEntity().setEnabled(true);
            userRepository.save(userGet);
            return userGet.getUserDetailsEntity();
        }else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    public UserDetails loadUserByUsernameGitHub(String username)throws UsernameNotFoundException {

        Optional<User> userOptional = userRepository.findByUserDetailsEntity_UsernameGitHub(username);
        if (userOptional.isPresent()) {
            User userGet = userOptional.get();
            userGet.getUserDetailsEntity().setEnabled(true);
            userRepository.save(userGet);
            return userGet.getUserDetailsEntity();
        }else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    public UserDetails loadByEmail(String email) throws UsernameNotFoundException{
        Optional<User> userOptional = userRepository.findByMail(email);
        if (userOptional.isPresent()) {
            User userGet = userOptional.get();
            userGet.getUserDetailsEntity().setEnabled(true);
            userRepository.save(userGet);
            return userGet.getUserDetailsEntity();
        }else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    public List<Cookie> removeCookies (HttpServletRequest request){
        try{
            CookieUtil cookieUtil = new CookieUtil();
            Cookie jwt =
                    cookieUtil.getCookie(request, "JWT");
            Cookie jsession =
                    cookieUtil.getCookie(request, "JSESSIONID");
            jwt.setMaxAge(0);
            jsession.setMaxAge(0);
            return List.of(jwt,jsession);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }


//    public boolean twoFactorAuthentication(UserDetails userDetails) {
//
//    }

}

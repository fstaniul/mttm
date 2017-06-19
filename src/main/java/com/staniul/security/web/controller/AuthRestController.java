package com.staniul.security.web.controller;

import com.staniul.security.jwt.JWTTokenProvider;
import com.staniul.teamspeak.query.Client;
import com.staniul.teamspeak.query.Query;
import com.staniul.teamspeak.query.QueryException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class AuthRestController {
    private static Logger log = Logger.getLogger(AuthRestController.class);

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    private final Query query;
    private final JWTTokenProvider tokenProvider;

    @Autowired
    public AuthRestController(Query query, JWTTokenProvider tokenProvider) {
        this.query = query;
        this.tokenProvider = tokenProvider;
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    @RequestMapping(value = "/api/auth", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> authenticateUser (HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        log.info("Called auth end point, ip address is: " + clientIp);
        try {
            List<Client> clients = query.getClientList().stream().filter(client -> client.getIp().equals(clientIp)).collect(Collectors.toList());
            if (clients.size() > 1)
                return ResponseEntity.badRequest().body("More then one client with this ip!");
            else {
                Client client = clients.get(0);
                String token = tokenProvider.generateToken(client, TimeUnit.DAYS.toMillis(1));
                TokenContainer tokenContainer = new TokenContainer(token);
                return ResponseEntity.ok(tokenContainer);
            }
        } catch (QueryException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}

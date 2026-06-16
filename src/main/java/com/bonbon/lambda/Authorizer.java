package com.bonbon.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.Map;

public class Authorizer implements RequestHandler<APIGatewayV2CustomAuthorizerEvent, Map<String, Object>> {

    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        try (SsmClient ssm = SsmClient.create()) {
            return ssm.getParameter(r -> r.name("/bonbon/api-key").withDecryption(true))
                    .parameter().value();
        }
    }

    @Override
    public Map<String, Object> handleRequest(APIGatewayV2CustomAuthorizerEvent event, Context context) {
        Map<String, String> headers = event.getHeaders();
        String providedKey = headers != null ? headers.get("x-api-key") : null;
        boolean authorized = API_KEY.equals(providedKey);
        return Map.of("isAuthorized", authorized);
    }
}

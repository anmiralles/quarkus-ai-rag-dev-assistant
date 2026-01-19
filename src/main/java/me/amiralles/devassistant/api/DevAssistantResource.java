package me.amiralles.devassistant.api;

import me.amiralles.devassistant.ai.DevAssistant;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/copilot")
public class DevAssistantResource {

    @Inject
    DevAssistant copilot;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ChatResponse ask(@QueryParam("q") String question) {
        if (question == null || question.trim().isEmpty()) {
            return new ChatResponse("Please provide a question about development topics.");
        }

        String answer = copilot.chat(question);
        return new ChatResponse(answer);
    }

    public static record ChatResponse(String answer) {
    }
}
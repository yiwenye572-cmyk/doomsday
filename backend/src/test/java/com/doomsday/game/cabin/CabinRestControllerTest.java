package com.doomsday.game.cabin;

import com.doomsday.game.cabin.dto.CabinRestRequest;
import com.doomsday.game.cabin.dto.CabinRestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CabinRestController.class)
public class CabinRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CabinRestService cabinRestService;

	@Test
	public void testRestEndpoint() throws Exception {
		CabinRestResponse resp = new CabinRestResponse("sess-1", 150, "afternoon");
		when(cabinRestService.rest(any(CabinRestRequest.class))).thenReturn(resp);

		CabinRestRequest req = new CabinRestRequest();
		req.setSessionId("sess-1");
		req.setDurationHours(5);
		req.setNewTimeOfDay("afternoon");

		mockMvc.perform(post("/api/v1/game/cabin/rest")
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.updatedStamina").value(150))
				.andExpect(jsonPath("$.data.updatedTimeOfDay").value("afternoon"));
	}
}
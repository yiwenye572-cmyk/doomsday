package com.doomsday.game.cabin;

import com.doomsday.game.cabin.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CabinController.class)
public class CabinRestControllerTest {

	private static final String SESSION = "sess-1";
	private static final String BASE = "/api/v1/game/sessions/" + SESSION + "/cabin";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CabinService cabinService;

	@MockBean
	private CabinRestService cabinRestService;

	@Test
	void testGetCabinState() throws Exception {
		CabinStateResponse resp = new CabinStateResponse(SESSION, 0L, "{}", 100, "morning");
		when(cabinService.getState(SESSION)).thenReturn(resp);

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.sessionId").value(SESSION))
				.andExpect(jsonPath("$.data.version").value(0));
	}

	@Test
	void testUpdateCabinState_success() throws Exception {
		CabinUpdateResponse resp = new CabinUpdateResponse(SESSION, 1L, "{}");
		when(cabinService.updateState(eq(SESSION), any(CabinUpdateRequest.class))).thenReturn(resp);

		CabinUpdateRequest req = new CabinUpdateRequest();
		req.setIdempotencyKey("idem-1");
		req.setExpectedVersion(0L);
		req.setChanges(List.of(Map.of("op", "move", "itemId", "bed_01", "payload", Map.of("x", 32, "y", 64))));

		mockMvc.perform(post(BASE + "/update")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.newVersion").value(1))
				.andExpect(jsonPath("$.data.conflict").value(false));
	}

	@Test
	void testUpdateCabinState_conflict() throws Exception {
		CabinUpdateResponse resp = CabinUpdateResponse.conflict(SESSION, 5L, "{}");
		when(cabinService.updateState(eq(SESSION), any(CabinUpdateRequest.class))).thenReturn(resp);

		CabinUpdateRequest req = new CabinUpdateRequest();
		req.setIdempotencyKey("idem-2");
		req.setExpectedVersion(0L);
		req.setChanges(List.of());

		mockMvc.perform(post(BASE + "/update")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONFLICT_VERSION"))
				.andExpect(jsonPath("$.data.conflict").value(true));
	}

	@Test
	void testRestEndpoint() throws Exception {
		CabinRestResponse resp = new CabinRestResponse(SESSION, 150, "afternoon");
		when(cabinRestService.rest(any(CabinRestRequest.class))).thenReturn(resp);

		CabinRestRequest req = new CabinRestRequest();
		req.setSessionId(SESSION);
		req.setDurationHours(5);
		req.setNewTimeOfDay("afternoon");

		mockMvc.perform(post(BASE + "/rest")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.data.updatedStamina").value(150))
				.andExpect(jsonPath("$.data.updatedTimeOfDay").value("afternoon"));
	}
}

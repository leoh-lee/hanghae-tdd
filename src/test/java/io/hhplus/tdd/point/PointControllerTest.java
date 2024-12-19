package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 ID로 포인트를 조회한다.")
    void point() throws Exception {
        // given
        long userId = 1L;
        int point = 1_000;

        when(pointService.getUserPointByUserId(userId)).thenReturn(new UserPoint(userId, point, 100000L));

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.get("/point/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(point));
    }

    @Test
    @DisplayName("사용자 ID로 포인트 충전/사용 내역을 조회한다.")
    void histories() throws Exception {
        // given
        long userId = 1L;

        when(pointService.getPointHistoriesByUserId(userId)).thenReturn(
                List.of(
                        new PointHistory(1L, userId, 10_000L, CHARGE, 100000L),
                        new PointHistory(2L, userId, 20_000L, USE, 100000L)
                )
        );

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.get("/point/" + userId +"/histories"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].userId").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].amount").value(10_000L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].type").value(CHARGE.name()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].id").value(2L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].userId").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].amount").value(20_000L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].type").value(USE.name()));
    }

    @Test
    @DisplayName("포인트 충전에 성공한다.")
    void charge() throws Exception {
        // given
        long userId = 1L;

        long amount = 10_000L;

        when(pointService.chargeUserPoint(eq(userId), eq(amount), anyLong())).thenReturn(
                new UserPoint(userId, amount, 100000L)
        );

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.patch("/point/" + userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(amount));
    }

    @Test
    @DisplayName("포인트 사용에 성공한다.")
    void use() throws Exception {
        // given
        long userId = 1L;
        long amount = 10_000L;

        when(pointService.usePoint(eq(userId), eq(amount), anyLong())).thenReturn(
                new UserPoint(userId, amount, 100000L)
        );

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.patch("/point/" + userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(amount));
    }

}
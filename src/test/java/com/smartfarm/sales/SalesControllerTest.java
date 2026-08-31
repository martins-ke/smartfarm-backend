package com.smartfarm.sales;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartfarm.ApiResponse;
import com.smartfarm.GlobalExceptionHandler;
import com.smartfarm.customers.Customer;
import com.smartfarm.projects.Project;

@ExtendWith(MockitoExtension.class)
class SalesControllerTest {

	private MockMvc mockMvc;

	@Mock
	private SalesService salesService;

	@InjectMocks
	private SalesController salesController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(salesController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void createSale_returnsCreatedResponse() throws Exception {
		String validPayload = """
				{
				  "item": "Milk",
				  "quantity": 20.0,
				  "unit_price": 75.0,
				  "project_id": "P001",
				  "customer": {
				    "name": "School cafeteria",
				    "contact": "0784463737",
				    "id_number": "12345678",
				    "address": "Kitale",
				    "status": "new"
				  }
				}
				""";

		Project p = new Project();
		p.setId("P001");
		Customer c = new Customer("C001", "School cafeteria", "0784463737", "12345678", "Kitale", true);
		Sale createdSale = new Sale("S001", "Milk", 20.0f, new BigDecimal("75.00"), LocalDate.now(), new BigDecimal("1500.00"), p, c);

		when(salesService.createSale(any(CreateSaleRequest.class)))
				.thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(createdSale, "Sale recorded successfully ✅", true, Instant.now())));

		mockMvc.perform(post("/sales/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validPayload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Sale recorded successfully ✅"))
				.andExpect(jsonPath("$.body.id").value("S001"))
				.andExpect(jsonPath("$.body.item").value("Milk"))
				.andExpect(jsonPath("$.body.project_id").value("P001"))
				.andExpect(jsonPath("$.body.customer.name").value("School cafeteria"))
				.andExpect(jsonPath("$.body.customer.id_number").value("12345678"));
	}

	@Test
	void createSale_withInvalidData_returnsBadRequest() throws Exception {
		String invalidPayload = """
				{
				  "item": "",
				  "quantity": -5.0,
				  "unit_price": -10.0,
				  "project_id": null
				}
				""";

		mockMvc.perform(post("/sales/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidPayload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void getSalesByProjectId_returnsList() throws Exception {
		Project p = new Project();
		p.setId("P001");
		Sale s = new Sale("S001", "Milk", 20.0f, new BigDecimal("75.00"), LocalDate.now(), new BigDecimal("1500.00"), p, null);

		when(salesService.getSalesByProjectId("P001", 0, 10)) 
				.thenReturn(ResponseEntity.ok(new ApiResponse<>(List.of(s), "Project sales fetched successfully", true, Instant.now())));

		mockMvc.perform(get("/sales/project/P001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body[0].item").value("Milk"))
				.andExpect(jsonPath("$.body[0].project_id").value("P001"));
	}

	@Test
	void deleteSale_returnsOk() throws Exception {
		when(salesService.deleteSale("S001"))
				.thenReturn(ResponseEntity.ok(new ApiResponse<>(null, "Sale deleted successfully", true, Instant.now())));

		mockMvc.perform(delete("/sales/S001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}
}

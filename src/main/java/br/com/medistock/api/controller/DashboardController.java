package br.com.medistock.api.controller;

import br.com.medistock.api.dto.response.DashboardResponse;
import br.com.medistock.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Resumo de estoque, pedidos do dia e alertas recentes")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo de estoque, pedidos do dia e alertas recentes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo do dia"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public DashboardResponse resumo() {
        return service.montarResumo();
    }
}

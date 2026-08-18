package br.com.medistock.api.controller;

import br.com.medistock.api.dto.response.PrevisaoResponse;
import br.com.medistock.api.service.PrevisaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Previsões", description = "Previsão de demanda a partir do histórico de saídas")
@RestController
@RequestMapping("/api/v1/previsoes")
public class PrevisaoController {
    private final PrevisaoService service;

    public PrevisaoController(PrevisaoService service) {
        this.service = service;
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Última previsão gerada para o item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Previsão encontrada"),
            @ApiResponse(responseCode = "404", description = "Item inexistente ou nenhuma previsão gerada ainda")
    })
    public PrevisaoResponse buscarPorItem(@PathVariable String itemId) {
        return service.buscarPorItem(itemId);
    }

    @PostMapping("/{itemId}/gerar")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','FARMACEUTICO')")
    @Operation(summary = "Calcula e salva uma nova previsão de demanda")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Previsão gerada"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    public PrevisaoResponse gerar(@PathVariable String itemId) {
        return service.gerar(itemId);
    }
}

package br.com.medistock.api.controller;

import br.com.medistock.api.dto.request.AlertaRequest;
import br.com.medistock.api.dto.response.AlertaResponse;
import br.com.medistock.api.model.enums.SeveridadeAlerta;
import br.com.medistock.api.model.enums.StatusAlerta;
import br.com.medistock.api.model.enums.TipoAlerta;
import br.com.medistock.api.service.AlertaService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Tag(name = "Alertas", description = "Alertas manuais e geração automática pela regra 6")
@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {
    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista alertas com filtros")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de alertas")
    })
    public List<AlertaResponse> listar(@RequestParam(required = false) TipoAlerta tipo,
                                       @RequestParam(required = false) SeveridadeAlerta severidade,
                                       @RequestParam(required = false) StatusAlerta status) {
        return service.listar(tipo, severidade, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um alerta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta encontrado"),
            @ApiResponse(responseCode = "404", description = "Alerta não encontrado")
    })
    public AlertaResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Cria um alerta manual")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alerta criado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
            @ApiResponse(responseCode = "422", description = "Item ou pedido referenciada não existe")
    })
    public ResponseEntity<AlertaResponse> criar(@Valid @RequestBody AlertaRequest request,
                                                UriComponentsBuilder uriBuilder) {
        AlertaResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/alertas/{id}").build(criado.id()))
                .body(criado);
    }

    @PostMapping("/gerar")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Varre estoque e pedidos e gera alertas automáticos (regra 6)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista dos alertas criados, vazia se não houver novidade"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão")
    })
    public List<AlertaResponse> gerar() {
        return service.gerar();
    }

    @PatchMapping("/{id}/resolver")
    @Operation(summary = "Marca um alerta como resolvido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta resolvido"),
            @ApiResponse(responseCode = "404", description = "Alerta não encontrado")
    })
    public AlertaResponse resolver(@PathVariable String id) {
        return service.resolver(id);
    }

    @PatchMapping("/{id}/ignorar")
    @Operation(summary = "Marca um alerta como ignorado, sem tratar a causa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta ignorado"),
            @ApiResponse(responseCode = "404", description = "Alerta não encontrado")
    })
    public AlertaResponse ignorar(@PathVariable String id) {
        return service.ignorar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove um alerta")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Alerta removido"),
            @ApiResponse(responseCode = "403", description = "Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Alerta não encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable String id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}

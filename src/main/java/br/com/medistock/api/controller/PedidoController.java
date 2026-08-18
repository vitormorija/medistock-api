package br.com.medistock.api.controller;

import br.com.medistock.api.dto.request.AtualizacaoStatusRequest;
import br.com.medistock.api.dto.request.PedidoRequest;
import br.com.medistock.api.dto.response.PedidoResponse;
import br.com.medistock.api.model.enums.StatusPedido;
import br.com.medistock.api.service.PedidoService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import br.com.medistock.api.security.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@Tag(name = "Pedidos", description = "Pedidos aos fornecedores e entrada no estoque")
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista pedidos com filtros")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos")
    })
    public List<PedidoResponse> listar(@RequestParam(required = false) StatusPedido status,
                                        @RequestParam(required = false) String fornecedorId) {
        return service.listar(status, fornecedorId);
    }

    @GetMapping("/atrasados")
    @Operation(summary = "Pedidos com SLA excedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos atrasados")
    })
    public List<PedidoResponse> listarAtrasados() {
        return service.listarAtrasados();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrada"),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrada")
    })
    public PedidoResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','FARMACEUTICO')")
    @Operation(summary = "Cria um pedido, com código gerado pelo sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado"),
            @ApiResponse(responseCode = "400", description = "Falha de validação nos itens"),
            @ApiResponse(responseCode = "422", description = "Fornecedor inexistente, inativo, ou item inexistente")
    })
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        PedidoResponse criada = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/pedidos/{id}").build(criada.id()))
                .body(criada);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Atualiza o status do pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
            @ApiResponse(responseCode = "422", description = "ENTREGUE só pode ser atingido por /confirmar")
    })
    public PedidoResponse atualizarStatus(@PathVariable String id,
                                           @Valid @RequestBody AtualizacaoStatusRequest request) {
        return service.atualizarStatus(id, request);
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','FARMACEUTICO')")
    @Operation(summary = "Confirma o recebimento e dá entrada no estoque (regra 4)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido confirmado e estoque atualizado"),
            @ApiResponse(responseCode = "422", description = "Pedido já confirmada ou cancelada")
    })
    public PedidoResponse confirmar(@PathVariable String id,
                                    @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return service.confirmar(id, autenticado.getUsuario().getId());
    }
}

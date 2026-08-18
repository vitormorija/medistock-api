package br.com.medistock.api.controller;

import br.com.medistock.api.dto.request.AjusteQuantidadeRequest;
import br.com.medistock.api.dto.request.ItemAtualizacaoRequest;
import br.com.medistock.api.dto.request.ItemCriacaoRequest;
import br.com.medistock.api.dto.response.ItemResponse;
import br.com.medistock.api.dto.response.PaginaResponse;
import br.com.medistock.api.model.enums.StatusItem;
import br.com.medistock.api.service.ItemService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import br.com.medistock.api.security.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Tag(name = "Itens", description = "Estoque de itens hospitalares, com status derivado")
@RestController
@RequestMapping("/api/v1/itens")
public class ItemController {
    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista itens com filtros e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de itens"),
            @ApiResponse(responseCode = "400", description = "Parâmetro de consulta inválido")
    })
    public PaginaResponse<ItemResponse> listar(
            @RequestParam(required = false) StatusItem status,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(status, categoria, busca, page, size);
    }

    @GetMapping("/criticos")
    @Operation(summary = "Itens com quantidade em nível crítico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de itens críticos")
    })
    public List<ItemResponse> listarCriticos() {
        return service.listarPorStatus(StatusItem.CRITICO);
    }

    @GetMapping("/vencendo")
    @Operation(summary = "Itens com validade em até 30 dias")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de itens vencendo")
    })
    public List<ItemResponse> listarVencendo() {
        return service.listarVencendo();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item encontrado"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    public ItemResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','FARMACEUTICO')")
    @Operation(summary = "Cadastra um item")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item cadastrado"),
            @ApiResponse(responseCode = "400", description = "Falha de validação, inclusive validade no passado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão")
    })
    public ResponseEntity<ItemResponse> criar(@Valid @RequestBody ItemCriacaoRequest request,
                                                UriComponentsBuilder uriBuilder) {
        ItemResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/itens/{id}").build(criado.id()))
                .body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','FARMACEUTICO')")
    @Operation(summary = "Atualiza um item integralmente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    public ItemResponse atualizar(@PathVariable String id,
                                    @Valid @RequestBody ItemAtualizacaoRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/quantidade")
    @Operation(summary = "Registra entrada ou saída de estoque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade ajustada e movimentação registrada"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado"),
            @ApiResponse(responseCode = "422", description = "Saída maior que o estoque disponível (regra 2)")
    })
    public ItemResponse ajustarQuantidade(@PathVariable String id,
                                            @Valid @RequestBody AjusteQuantidadeRequest request,
                                            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return service.ajustarQuantidade(id, request, autenticado.getUsuario().getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove um item")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removido"),
            @ApiResponse(responseCode = "403", description = "Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable String id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}

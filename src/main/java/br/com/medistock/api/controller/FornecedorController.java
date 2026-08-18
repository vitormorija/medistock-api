package br.com.medistock.api.controller;

import br.com.medistock.api.dto.request.FornecedorRequest;
import br.com.medistock.api.dto.response.FornecedorResponse;
import br.com.medistock.api.service.FornecedorService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Tag(name = "Fornecedores", description = "Cadastro de fornecedores e exclusão lógica")
@RestController
@RequestMapping("/api/v1/fornecedores")
public class FornecedorController {
    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos os fornecedores")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de fornecedores")
    })
    public List<FornecedorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor encontrado"),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado")
    })
    public FornecedorResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Cadastra um fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fornecedor cadastrado"),
            @ApiResponse(responseCode = "400", description = "CNPJ fora do formato de 14 dígitos"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão"),
            @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado (regra 7)")
    })
    public ResponseEntity<FornecedorResponse> criar(@Valid @RequestBody FornecedorRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        FornecedorResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/fornecedores/{id}").build(criado.id()))
                .body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    @Operation(summary = "Atualiza um fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor atualizado"),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado"),
            @ApiResponse(responseCode = "409", description = "CNPJ de outro fornecedor")
    })
    public FornecedorResponse atualizar(@PathVariable String id,
                                        @Valid @RequestBody FornecedorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativa um fornecedor (exclusão lógica)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fornecedor inativado, documento preservado"),
            @ApiResponse(responseCode = "403", description = "Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado")
    })
    public ResponseEntity<Void> inativar(@PathVariable String id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}

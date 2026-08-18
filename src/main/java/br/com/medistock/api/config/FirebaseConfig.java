package br.com.medistock.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "medistock.persistencia", havingValue = "firestore", matchIfMissing = true)
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp(@Value("${medistock.firebase.credenciais}") String caminho)
            throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        Path arquivo = Path.of(caminho);
        if (!Files.isReadable(arquivo)) {
            throw new IOException("""
                    Credenciais do Firebase nao encontradas em: %s
                    Baixe a chave da conta de servico no console do Firebase
                    (Configuracoes do projeto -> Contas de servico) e salve nesse caminho,
                    ou aponte a variavel de ambiente FIREBASE_CREDENTIALS para ela.
                    Para rodar sem o Firebase, use PERSISTENCIA=memoria.""".formatted(arquivo.toAbsolutePath()));
        }

        try (InputStream credenciais = new FileInputStream(arquivo.toFile())) {
            FirebaseOptions opcoes = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credenciais))
                    .build();

            FirebaseApp aplicacao = FirebaseApp.initializeApp(opcoes);
            log.info("Firebase inicializado a partir de {}", arquivo.toAbsolutePath());
            return aplicacao;
        }
    }

    @Bean
    public Firestore firestore(FirebaseApp aplicacao) {
        return FirestoreClient.getFirestore(aplicacao);
    }
}

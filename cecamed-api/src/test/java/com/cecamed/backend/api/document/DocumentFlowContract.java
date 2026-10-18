package com.cecamed.backend.api.document;

import com.cecamed.backend.application.identity.port.in.UserManagementUseCase;
import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.domain.identity.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class DocumentFlowContract {
    @TempDir static Path directory;
    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry) {
        registry.add("cecamed.documents.directory", () -> directory.toString());
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserManagementUseCase users;
    String token;
    long patientId;
    static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    @BeforeEach void prepare() throws Exception {
        login(Role.MEDICO);
        patientId = call(post("/api/v1/patients").contentType(MediaType.APPLICATION_JSON).content("""
            {"firstName":"Ana","lastName":"Documentos","birthDate":"2000-01-02","gender":"FEMENINO",
             "bloodType":"DESCONOCIDO","phone":"55551234","allergies":"Ninguna"}
            """), 201).get("id").asLong();
    }
    void login(Role role) throws Exception {
        String username="doc"+UUID.randomUUID().toString().replace("-", "");
        users.createUser(new CreateUserCommand(username,"Document-Test-482!","Prueba",role,"test","127.0.0.1","document-test"));
        token=call(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", "Document-Test-482!"))),200)
                .get("accessToken").asText();
    }
    JsonNode call(MockHttpServletRequestBuilder request,int expected) throws Exception {
        if (token != null) request.header("Authorization", "Bearer "+token);
        String body=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isBlank()?null:json.readTree(body);
    }
    String path() { return "/api/v1/patients/"+patientId+"/documents"; }
    JsonNode upload(String filename,byte[] data,int expected) throws Exception {
        return call(multipart(path()).file(new MockMultipartFile("file",filename,"application/octet-stream",data))
                .param("documentType","ESTUDIO_LABORATORIO").param("description","  Resultado  "),expected);
    }
    @Test void uploadListDownloadAndDeletePersistedBytes() throws Exception {
        var doc=upload("../../resultado.pdf",PDF,201);
        long id=doc.get("id").asLong();
        assertThat(doc.get("originalFileName").asText()).isEqualTo("resultado.pdf");
        assertThat(doc.get("description").asText()).isEqualTo("Resultado");
        assertThat(doc.has("storageKey")).isFalse();
        assertThat(doc.get("checksumSha256").asText()).hasSize(64);
        assertThat(call(get(path()),200).size()).isEqualTo(1);
        mvc.perform(get("/api/v1/documents/"+id+"/file").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(get(path()+"/"+id+"/content").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF));
        call(get("/api/v1/patients/999999999/documents/"+id+"/content"),404);
        call(post("/api/v1/patients/999999999/documents/"+id+"/deactivate"),404);
        call(delete("/api/v1/documents/"+id),204);
        assertThat(call(get(path()),200).size()).isZero();
        call(get("/api/v1/documents/"+id+"/file"),404);
        call(delete("/api/v1/documents/"+id),204);
        try (var files=Files.list(directory)) { assertThat(files.toList()).isEmpty(); }
    }
    @Test void validatesContentMetadataAndPatient() throws Exception {
        upload("empty.pdf",new byte[0],400);
        upload("fake.pdf","<html>not a pdf</html>".getBytes(),400);
        upload("mismatch.png",PDF,400);
        upload("big.pdf",new byte[15*1024*1024+1],413);
        call(multipart(path()).file(new MockMultipartFile("file","ok.pdf","application/pdf",PDF))
                .param("documentType","INVALID"),400);
        call(multipart(path()).param("documentType","OTRO"),400);
        call(multipart(path()).file(new MockMultipartFile("file","ok.pdf","application/pdf",PDF))
                .param("documentType","OTRO").param("description","x".repeat(2001)),400);
        call(patch("/api/v1/patients/"+patientId+"/deactivate").param("version","0"),204);
        upload("inactive.pdf",PDF,409);
        patientId=999999999;
        upload("missing.pdf",PDF,404);
        call(get(path()),404);
        call(get("/api/v1/documents/999999999/file"),404);
        try (var files=Files.list(directory)) { assertThat(files.toList()).isEmpty(); }
    }
    @Test void acceptsImagesAndNestedDeletion() throws Exception {
        for (String format : List.of("png", "jpg")) {
            var bytes=new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),format,bytes);
            var doc=upload("imagen."+format,bytes.toByteArray(),201);
            call(post(path()+"/"+doc.get("id").asLong()+"/deactivate"),204);
        }
    }
    @Test void clinicalDocumentsAreForbiddenToEveryOtherRole() throws Exception {
        var doc=upload("private.pdf",PDF,201);
        long id=doc.get("id").asLong();
        String doctorToken=token;
        for (Role role : List.of(Role.RECEPCION, Role.ADMIN, Role.PACIENTE)) {
            login(role);
            call(get(path()),403);
            upload("private.pdf",PDF,403);
            call(get("/api/v1/documents/"+id+"/file"),403);
            call(delete("/api/v1/documents/"+id),403);
            call(get(path()+"/"+id+"/content"),403);
            call(post(path()+"/"+id+"/deactivate"),403);
        }
        token=null;
        call(get(path()),401);
        call(get("/api/v1/documents/"+id+"/file"),401);
        call(delete("/api/v1/documents/"+id),401);
        token=doctorToken;
        call(delete("/api/v1/documents/"+id),204);
    }
}

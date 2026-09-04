package com.example.wanteat.controller;

import com.example.wanteat.dto.SampleRequest;
import com.example.wanteat.dto.SampleResponse;
import com.example.wanteat.service.SampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// TODO: アプリのドメインに合わせてリネーム・パスを修正する
@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

    @GetMapping
    public ResponseEntity<List<SampleResponse>> getAll() {
        return ResponseEntity.ok(sampleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SampleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sampleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<SampleResponse> create(@Valid @RequestBody SampleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sampleService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SampleResponse> update(
            @PathVariable Long id, @Valid @RequestBody SampleRequest request) {
        return ResponseEntity.ok(sampleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sampleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

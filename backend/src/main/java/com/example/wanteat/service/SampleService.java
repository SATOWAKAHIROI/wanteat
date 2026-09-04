package com.example.wanteat.service;

import com.example.wanteat.domain.Sample;
import com.example.wanteat.dto.SampleRequest;
import com.example.wanteat.dto.SampleResponse;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

// TODO: アプリのドメインに合わせてリネーム・修正する
@Service
@RequiredArgsConstructor
public class SampleService {

    private final SampleRepository sampleRepository;

    public List<SampleResponse> findAll() {
        return sampleRepository.findAll().stream().map(SampleResponse::from).toList();
    }

    public SampleResponse findById(Long id) {
        return SampleResponse.from(findOrThrow(id));
    }

    public SampleResponse create(SampleRequest request) {
        Sample sample = new Sample();
        sample.setName(request.name());
        // TODO: フィールドをセットする
        return SampleResponse.from(sampleRepository.save(sample));
    }

    public SampleResponse update(Long id, SampleRequest request) {
        Sample sample = findOrThrow(id);
        sample.setName(request.name());
        // TODO: フィールドをセットする
        return SampleResponse.from(sampleRepository.save(sample));
    }

    public void delete(Long id) {
        if (!sampleRepository.existsById(id)) throw new NotFoundException("リソースが見つかりません");
        sampleRepository.deleteById(id);
    }

    private Sample findOrThrow(Long id) {
        return sampleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("リソースが見つかりません"));
    }
}

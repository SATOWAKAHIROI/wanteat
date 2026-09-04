package com.example.wanteat.repository;

import com.example.wanteat.domain.Sample;
import org.springframework.data.jpa.repository.JpaRepository;

// TODO: アプリのドメインに合わせてリネーム・修正する
public interface SampleRepository extends JpaRepository<Sample, Long> {
}

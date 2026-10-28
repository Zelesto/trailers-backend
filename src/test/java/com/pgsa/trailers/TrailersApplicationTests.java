package com.pgsa.trailers;

import com.pgsa.trailers.entity.Sequence;
import com.pgsa.trailers.repository.SequenceRepository;
import com.pgsa.trailers.service.SequenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrailersApplicationTests {

    @Mock
    private SequenceRepository sequenceRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SequenceService sequenceService;

    @Test
    void contextLoads() {
    }

    @Test
    void initSequencesCreatesFallbackSequenceTableWhenMissing() {
        doThrow(new EmptyResultDataAccessException(1)).when(jdbcTemplate)
                .queryForObject(anyString(), eq(Integer.class));
        when(sequenceRepository.findByTableNameAndYear("trip", "2026")).thenReturn(Optional.empty());
        when(sequenceRepository.findByTableNameAndYear("load", "2026")).thenReturn(Optional.empty());
        when(sequenceRepository.findByTableNameAndYear("pod", "2026")).thenReturn(Optional.empty());
        when(sequenceRepository.findByTableNameAndYear("customer", "2026")).thenReturn(Optional.empty());
        when(sequenceRepository.findByTableNameAndYear("invoice", "2026")).thenReturn(Optional.empty());
        when(sequenceRepository.save(any(Sequence.class)))
                .thenAnswer(invocation -> {
                    Sequence sequence = invocation.getArgument(0);
                    sequence.setId(1L);
                    sequence.setCreatedAt(LocalDateTime.now());
                    sequence.setUpdatedAt(LocalDateTime.now());
                    return sequence;
                });

        sequenceService.initSequences();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE IF NOT EXISTS sequence"));
    }

    @Test
    void initSequencesCreatesExpenseSequenceRow() {
        when(sequenceRepository.findByTableNameAndYear("trip", "2026")).thenReturn(Optional.of(new Sequence()));
        when(sequenceRepository.findByTableNameAndYear("load", "2026")).thenReturn(Optional.of(new Sequence()));
        when(sequenceRepository.findByTableNameAndYear("pod", "2026")).thenReturn(Optional.of(new Sequence()));
        when(sequenceRepository.findByTableNameAndYear("customer", "2026")).thenReturn(Optional.of(new Sequence()));
        when(sequenceRepository.findByTableNameAndYear("invoice", "2026")).thenReturn(Optional.of(new Sequence()));
        when(sequenceRepository.findByTableNameAndYear("expenses", "2026")).thenReturn(Optional.empty());
        when(sequenceRepository.save(any(Sequence.class)))
                .thenAnswer(invocation -> {
                    Sequence sequence = invocation.getArgument(0);
                    sequence.setId(1L);
                    sequence.setCreatedAt(LocalDateTime.now());
                    sequence.setUpdatedAt(LocalDateTime.now());
                    return sequence;
                });

        sequenceService.initSequences();

        verify(sequenceRepository).save(argThat(sequence ->
                "expenses".equals(sequence.getTableName()) && "2026".equals(sequence.getYear())));
    }
}

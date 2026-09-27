package com.recruitment.job.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(indexName = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long jobId;

    @Field(type = FieldType.Keyword)
    private UUID tenantId;

    @Field(type = FieldType.Keyword)
    private String companyName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Text)
    private String requirements;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Keyword)
    private String employmentType;

    @Field(type = FieldType.Keyword)
    private String experienceLevel;

    @Field(type = FieldType.Keyword)
    private String workType;

    @Field(type = FieldType.Double)
    private BigDecimal salaryMin;

    @Field(type = FieldType.Double)
    private BigDecimal salaryMax;

    @Field(type = FieldType.Keyword)
    private String salaryCurrency;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private List<String> skills;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date)
    private LocalDateTime publishedAt;

    @Field(type = FieldType.Date)
    private LocalDateTime expiresAt;
}

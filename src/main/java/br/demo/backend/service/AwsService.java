package br.demo.backend.service;

import br.demo.backend.model.Archive;
import br.demo.backend.model.Project;
import br.demo.backend.repository.ProjectRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@AllArgsConstructor
@Service
@Data
public class AwsService {

    private ProjectRepository projectRepository;
    private Environment env;


    public boolean uploadFile(MultipartFile file, Long id) {
        String keyID = env.getProperty("keyID");
        String keySecret = env.getProperty("keySecret");
        String region = "us-east-1";
        String bucketName = env.getProperty("bucket");
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(keyID, keySecret);

        try (S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(region))
                .build()) {

            if (!doesBucketExist(s3Client, bucketName)) {
                return false;
            }

            String fileKey = file.getOriginalFilename(); // Assumindo que você deseja usar o nome original do arquivo como chave
            String contentType = file.getContentType();
            String stringAws = UUID.randomUUID().toString();

            try (InputStream fileInputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(stringAws)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(fileInputStream, file.getSize()));
                Project project = projectRepository.findById(id).get();
                Archive archive = new Archive(file);
                archive.setAwsKey(stringAws);
                project.setPicture(archive);
                projectRepository.save(project);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String findById(long projectId) {
        String keyID = env.getProperty("keyID");
        String keySecret = env.getProperty("keySecret");
        String region = "us-east-1";
        String bucketName = env.getProperty("bucket");
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(keyID, keySecret);
        S3Client s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(region))
                .build();
        if (doesBucketExist(s3Client, bucketName)) {
            try (S3Presigner presigner = S3Presigner.builder().region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build()) {

                String awsKey = projectRepository.findById(projectId).get().getPicture().getAwsKey();
                GetObjectRequest objectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(awsKey)
                        .build();

                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))  // The URL will expire in 10 minutes.
                        .getObjectRequest(objectRequest)
                        .build();

                PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

                return presignedRequest.url().toExternalForm();
            }
        }
        return null;
    }

    private boolean doesBucketExist(S3Client s3Client, String bucketName) {
        try {
            s3Client.headBucket(b -> b.bucket(bucketName));
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }
}

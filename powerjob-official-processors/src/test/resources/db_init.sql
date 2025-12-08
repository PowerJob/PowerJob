create table test_table
(
    id           bigint primary key,
    content      varchar(255),
    gmt_create   timestamp default current_timestamp,
    gmt_modified timestamp default current_timestamp
);

-- Create job_info table for dynamic datasource tests
CREATE TABLE job_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(255),
    job_description TEXT,
    job_params TEXT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert some sample data into job_info
INSERT INTO job_info (job_name, job_description, job_params) VALUES
('test-job-1', 'Test job 1 description', '{"param1": "value1"}'),
('test-job-2', 'Test job 2 description', '{"param2": "value2"}'),
('test-job-3', 'Test job 3 description', '{"param3": "value3"}');
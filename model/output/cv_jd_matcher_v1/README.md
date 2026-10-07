---
tags:
- sentence-transformers
- cross-encoder
- reranker
- generated_from_trainer
- dataset_size:94938
- loss:CrossEntropyLoss
base_model: cross-encoder/ms-marco-MiniLM-L6-v2
pipeline_tag: text-classification
library_name: sentence-transformers
metrics:
- f1_macro
- f1_micro
- f1_weighted
model-index:
- name: CrossEncoder based on cross-encoder/ms-marco-MiniLM-L6-v2
  results:
  - task:
      type: cross-encoder-softmax-accuracy
      name: Cross Encoder Softmax Accuracy
    dataset:
      name: val eval
      type: val_eval
    metrics:
    - type: f1_macro
      value: 0.6485341424899332
      name: F1 Macro
    - type: f1_micro
      value: 0.9248851184164015
      name: F1 Micro
    - type: f1_weighted
      value: 0.9224015679615915
      name: F1 Weighted
---

# CrossEncoder based on cross-encoder/ms-marco-MiniLM-L6-v2

This is a [Cross Encoder](https://www.sbert.net/docs/cross_encoder/usage/usage.html) model finetuned from [cross-encoder/ms-marco-MiniLM-L6-v2](https://huggingface.co/cross-encoder/ms-marco-MiniLM-L6-v2) using the [sentence-transformers](https://www.SBERT.net) library. It computes scores for pairs of texts, which can be used for text pair classification.

## Model Details

### Model Description
- **Model Type:** Cross Encoder
- **Base model:** [cross-encoder/ms-marco-MiniLM-L6-v2](https://huggingface.co/cross-encoder/ms-marco-MiniLM-L6-v2) <!-- at revision 233902d25c440f23af6f7d6e94d2946bac0bee0a -->
- **Maximum Sequence Length:** 512 tokens
- **Number of Output Labels:** 3 labels
<!-- - **Training Dataset:** Unknown -->
<!-- - **Language:** Unknown -->
<!-- - **License:** Unknown -->

### Model Sources

- **Documentation:** [Sentence Transformers Documentation](https://sbert.net)
- **Documentation:** [Cross Encoder Documentation](https://www.sbert.net/docs/cross_encoder/usage/usage.html)
- **Repository:** [Sentence Transformers on GitHub](https://github.com/UKPLab/sentence-transformers)
- **Hugging Face:** [Cross Encoders on Hugging Face](https://huggingface.co/models?library=sentence-transformers&other=cross-encoder)

## Usage

### Direct Usage (Sentence Transformers)

First install the Sentence Transformers library:

```bash
pip install -U sentence-transformers
```

Then you can load this model and run inference.
```python
from sentence_transformers import CrossEncoder

# Download from the 🤗 Hub
model = CrossEncoder("cross_encoder_model_id")
# Get scores for pairs of texts
pairs = [
    ['Candidate Profile\n\n\n\nSkills:\nPython, Power BI, ETL, Tableau, Pandas, Statistics\n\nExperience:\n- Data Analyst (1.0 years) | Skills: Python, Power BI, ETL, Tableau, Pandas\n- Delivered results using structured workflows and clear communication\n- Collaborated with stakeholders to define needs and execute tasks\n- Maintained reporting and documentation to support team performance\n\nEducation:\n- BSc in Travel', 'Job Title:\nBI Analyst\n\nRequired Skills:\nTableau, Pandas, Power BI, Python\n\nPreferred Skills:\nExcel'],
    ['Candidate Profile\n\n\n\nSkills:\nContent Marketing, Copywriting, A/B Testing, Marketing Analytics, Conversion Optimization, Meta Ads, Google Ads, Landing Pages\n\nExperience:\n- Content Marketer (12.0 years) | Skills: Content Marketing, Copywriting, A/B Testing, Marketing Analytics, Conversion Optimization\n- Delivered results using structured workflows and clear communication\n- Collaborated with stakeholders to define needs and execute tasks\n- Maintained reporting and documentation to support team performance\n\nEducation:\n- BSc in E-commerce', 'Job Title:\nMarketing Manager\n\nRequired Skills:\nCopywriting, Landing Pages, Meta Ads, A/B Testing, Marketing Analytics\n\nPreferred Skills:\nEmail Marketing'],
    ['Candidate Profile\n\nSkills:\nProficient in Embedded Systems, Problem Solving, Mechanical Engineering, Prototyping, Automation, with senior-level experience in the field. Holds a PhD degree. Holds certifications such as Certified Automation Professional (CAP). Skilled in delivering results and adapting to dynamic environments.', 'Job Title:\nRobotics Engineer\n\nRequired Skills:\n(see description)\n\nDescription:\nAs a Robotics Engineer, you will design, develop, and test robotic systems that automate tasks in industries such as manufacturing, healthcare, and logistics. Your role will involve programming robots, integrating sensors, and troubleshooting mechanical and software issues. This position requires expertise in mechanical engineering, computer science, and electrical systems. You will work closely with cross-disciplinary teams to create cutting-edge robotic solutions that improve productivity and efficiency. Innovation and problem-solving are essential as you develop robots that perform tasks ranging from assembly line work to medical surgeries.'],
    ['Candidate Profile\n\nSkills:\nTicketing Systems, Communication, SAP\n\nExperience:\n- 9 years\n\nCV:\nHighly motivated professional seeking the Technical Support Specialist position. I bring 9 years of hands-on experience in the field. My core competencies include: Ticketing Systems, Communication, SAP. I am confident that my extensive experience makes me an ideal fit for this role.', 'Job Title:\nTechnical Support Specialist\n\nRequired Skills:\nCustomer Support, Troubleshooting, Ticketing Systems, Communication\n\nDescription:\nProvide tier 2 technical support for software products and resolve customer issues.\n\nRequirements:\n2+ years in IT support, excellent communication skills, troubleshooting expertise.'],
    ['Candidate Profile\n\n\n\nSkills:\nLanding Pages, SEO, Meta Ads, Copywriting, Content Marketing\n\nExperience:\n- Marketing Manager (2.0 years) | Skills: Landing Pages, SEO, Meta Ads, Copywriting, Content Marketing\n- Delivered results using structured workflows and clear communication\n- Collaborated with stakeholders to define needs and execute tasks\n- Maintained reporting and documentation to support team performance\n\nEducation:\n- BSc in SaaS', 'Job Title:\nBackend Engineer\n\nRequired Skills:\nJavaScript, Docker, Unit Testing\n\nPreferred Skills:\nObject-Oriented Programming'],
]
scores = model.predict(pairs)
print(scores.shape)
# (5, 3)
```

<!--
### Direct Usage (Transformers)

<details><summary>Click to see the direct usage in Transformers</summary>

</details>
-->

<!--
### Downstream Usage (Sentence Transformers)

You can finetune this model on your own dataset.

<details><summary>Click to expand</summary>

</details>
-->

<!--
### Out-of-Scope Use

*List how the model may foreseeably be misused and address what users ought not to do with the model.*
-->

## Evaluation

### Metrics

#### Cross Encoder Softmax Accuracy

* Dataset: `val_eval`
* Evaluated with [<code>CESoftmaxAccuracyEvaluator</code>](https://sbert.net/docs/package_reference/cross_encoder/evaluation.html#sentence_transformers.cross_encoder.evaluation.CESoftmaxAccuracyEvaluator)

| Metric       | Value      |
|:-------------|:-----------|
| **f1_macro** | **0.6485** |
| f1_micro     | 0.9249     |
| f1_weighted  | 0.9224     |

<!--
## Bias, Risks and Limitations

*What are the known or foreseeable issues stemming from this model? You could also flag here known failure cases or weaknesses of the model.*
-->

<!--
### Recommendations

*What are recommendations with respect to the foreseeable issues? For example, filtering explicit content.*
-->

## Training Details

### Training Dataset

#### Unnamed Dataset

* Size: 94,938 training samples
* Columns: <code>sentence_0</code>, <code>sentence_1</code>, and <code>label</code>
* Approximate statistics based on the first 1000 samples:
  |         | sentence_0                                                                                        | sentence_1                                                                                       | label                                                             |
  |:--------|:--------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------|:------------------------------------------------------------------|
  | type    | string                                                                                            | string                                                                                           | int                                                               |
  | details | <ul><li>min: 228 characters</li><li>mean: 440.07 characters</li><li>max: 571 characters</li></ul> | <ul><li>min: 85 characters</li><li>mean: 191.04 characters</li><li>max: 847 characters</li></ul> | <ul><li>0: ~30.20%</li><li>1: ~1.10%</li><li>2: ~68.70%</li></ul> |
* Samples:
  | sentence_0                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | sentence_1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | label          |
  |:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------|
  | <code>Candidate Profile<br><br><br><br>Skills:<br>Python, Power BI, ETL, Tableau, Pandas, Statistics<br><br>Experience:<br>- Data Analyst (1.0 years) \| Skills: Python, Power BI, ETL, Tableau, Pandas<br>- Delivered results using structured workflows and clear communication<br>- Collaborated with stakeholders to define needs and execute tasks<br>- Maintained reporting and documentation to support team performance<br><br>Education:<br>- BSc in Travel</code>                                                                                                                                         | <code>Job Title:<br>BI Analyst<br><br>Required Skills:<br>Tableau, Pandas, Power BI, Python<br><br>Preferred Skills:<br>Excel</code>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | <code>2</code> |
  | <code>Candidate Profile<br><br><br><br>Skills:<br>Content Marketing, Copywriting, A/B Testing, Marketing Analytics, Conversion Optimization, Meta Ads, Google Ads, Landing Pages<br><br>Experience:<br>- Content Marketer (12.0 years) \| Skills: Content Marketing, Copywriting, A/B Testing, Marketing Analytics, Conversion Optimization<br>- Delivered results using structured workflows and clear communication<br>- Collaborated with stakeholders to define needs and execute tasks<br>- Maintained reporting and documentation to support team performance<br><br>Education:<br>- BSc in E-commerce</code> | <code>Job Title:<br>Marketing Manager<br><br>Required Skills:<br>Copywriting, Landing Pages, Meta Ads, A/B Testing, Marketing Analytics<br><br>Preferred Skills:<br>Email Marketing</code>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | <code>2</code> |
  | <code>Candidate Profile<br><br>Skills:<br>Proficient in Embedded Systems, Problem Solving, Mechanical Engineering, Prototyping, Automation, with senior-level experience in the field. Holds a PhD degree. Holds certifications such as Certified Automation Professional (CAP). Skilled in delivering results and adapting to dynamic environments.</code>                                                                                                                                                                                                                                                         | <code>Job Title:<br>Robotics Engineer<br><br>Required Skills:<br>(see description)<br><br>Description:<br>As a Robotics Engineer, you will design, develop, and test robotic systems that automate tasks in industries such as manufacturing, healthcare, and logistics. Your role will involve programming robots, integrating sensors, and troubleshooting mechanical and software issues. This position requires expertise in mechanical engineering, computer science, and electrical systems. You will work closely with cross-disciplinary teams to create cutting-edge robotic solutions that improve productivity and efficiency. Innovation and problem-solving are essential as you develop robots that perform tasks ranging from assembly line work to medical surgeries.</code> | <code>2</code> |
* Loss: [<code>CrossEntropyLoss</code>](https://sbert.net/docs/package_reference/cross_encoder/losses.html#crossentropyloss)

### Training Hyperparameters
#### Non-Default Hyperparameters

- `eval_strategy`: steps
- `per_device_train_batch_size`: 16
- `per_device_eval_batch_size`: 16

#### All Hyperparameters
<details><summary>Click to expand</summary>

- `overwrite_output_dir`: False
- `do_predict`: False
- `eval_strategy`: steps
- `prediction_loss_only`: True
- `per_device_train_batch_size`: 16
- `per_device_eval_batch_size`: 16
- `per_gpu_train_batch_size`: None
- `per_gpu_eval_batch_size`: None
- `gradient_accumulation_steps`: 1
- `eval_accumulation_steps`: None
- `torch_empty_cache_steps`: None
- `learning_rate`: 5e-05
- `weight_decay`: 0.0
- `adam_beta1`: 0.9
- `adam_beta2`: 0.999
- `adam_epsilon`: 1e-08
- `max_grad_norm`: 1
- `num_train_epochs`: 3
- `max_steps`: -1
- `lr_scheduler_type`: linear
- `lr_scheduler_kwargs`: {}
- `warmup_ratio`: 0.0
- `warmup_steps`: 0
- `log_level`: passive
- `log_level_replica`: warning
- `log_on_each_node`: True
- `logging_nan_inf_filter`: True
- `save_safetensors`: True
- `save_on_each_node`: False
- `save_only_model`: False
- `restore_callback_states_from_checkpoint`: False
- `no_cuda`: False
- `use_cpu`: False
- `use_mps_device`: False
- `seed`: 42
- `data_seed`: None
- `jit_mode_eval`: False
- `use_ipex`: False
- `bf16`: False
- `fp16`: False
- `fp16_opt_level`: O1
- `half_precision_backend`: auto
- `bf16_full_eval`: False
- `fp16_full_eval`: False
- `tf32`: None
- `local_rank`: 0
- `ddp_backend`: None
- `tpu_num_cores`: None
- `tpu_metrics_debug`: False
- `debug`: []
- `dataloader_drop_last`: False
- `dataloader_num_workers`: 0
- `dataloader_prefetch_factor`: None
- `past_index`: -1
- `disable_tqdm`: False
- `remove_unused_columns`: True
- `label_names`: None
- `load_best_model_at_end`: False
- `ignore_data_skip`: False
- `fsdp`: []
- `fsdp_min_num_params`: 0
- `fsdp_config`: {'min_num_params': 0, 'xla': False, 'xla_fsdp_v2': False, 'xla_fsdp_grad_ckpt': False}
- `fsdp_transformer_layer_cls_to_wrap`: None
- `accelerator_config`: {'split_batches': False, 'dispatch_batches': None, 'even_batches': True, 'use_seedable_sampler': True, 'non_blocking': False, 'gradient_accumulation_kwargs': None}
- `parallelism_config`: None
- `deepspeed`: None
- `label_smoothing_factor`: 0.0
- `optim`: adamw_torch_fused
- `optim_args`: None
- `adafactor`: False
- `group_by_length`: False
- `length_column_name`: length
- `ddp_find_unused_parameters`: None
- `ddp_bucket_cap_mb`: None
- `ddp_broadcast_buffers`: False
- `dataloader_pin_memory`: True
- `dataloader_persistent_workers`: False
- `skip_memory_metrics`: True
- `use_legacy_prediction_loop`: False
- `push_to_hub`: False
- `resume_from_checkpoint`: None
- `hub_model_id`: None
- `hub_strategy`: every_save
- `hub_private_repo`: None
- `hub_always_push`: False
- `hub_revision`: None
- `gradient_checkpointing`: False
- `gradient_checkpointing_kwargs`: None
- `include_inputs_for_metrics`: False
- `include_for_metrics`: []
- `eval_do_concat_batches`: True
- `fp16_backend`: auto
- `push_to_hub_model_id`: None
- `push_to_hub_organization`: None
- `mp_parameters`: 
- `auto_find_batch_size`: False
- `full_determinism`: False
- `torchdynamo`: None
- `ray_scope`: last
- `ddp_timeout`: 1800
- `torch_compile`: False
- `torch_compile_backend`: None
- `torch_compile_mode`: None
- `include_tokens_per_second`: False
- `include_num_input_tokens_seen`: False
- `neftune_noise_alpha`: None
- `optim_target_modules`: None
- `batch_eval_metrics`: False
- `eval_on_start`: False
- `use_liger_kernel`: False
- `liger_kernel_config`: None
- `eval_use_gather_object`: False
- `average_tokens_across_devices`: False
- `prompts`: None
- `batch_sampler`: batch_sampler
- `multi_dataset_batch_sampler`: proportional
- `router_mapping`: {}
- `learning_rate_mapping`: {}

</details>

### Training Logs
| Epoch  | Step | Training Loss | val_eval_f1_macro |
|:------:|:----:|:-------------:|:-----------------:|
| 0.0843 | 500  | 0.8133        | 0.5847            |
| 0.1685 | 1000 | 0.3012        | 0.6022            |
| 0.2528 | 1500 | 0.2133        | 0.6088            |
| 0.3370 | 2000 | 0.2244        | 0.6063            |
| 0.4213 | 2500 | 0.2052        | 0.6060            |
| 0.5056 | 3000 | 0.1974        | 0.6103            |
| 0.5898 | 3500 | 0.1911        | 0.6127            |
| 0.6741 | 4000 | 0.1809        | 0.6067            |
| 0.7583 | 4500 | 0.1899        | 0.6098            |
| 0.8426 | 5000 | 0.1992        | 0.6116            |
| 0.9269 | 5500 | 0.1841        | 0.6238            |
| 1.0    | 5934 | -             | 0.6089            |
| 1.0111 | 6000 | 0.1774        | 0.6058            |
| 1.0954 | 6500 | 0.1883        | 0.6119            |
| 1.1796 | 7000 | 0.1646        | 0.6090            |
| 1.2639 | 7500 | 0.1775        | 0.6440            |
| 1.3482 | 8000 | 0.1837        | 0.6485            |


### Framework Versions
- Python: 3.11.4
- Sentence Transformers: 5.1.1
- Transformers: 4.56.2
- PyTorch: 2.8.0+cpu
- Accelerate: 1.15.0
- Datasets: 5.0.1
- Tokenizers: 0.22.1

## Citation

### BibTeX

#### Sentence Transformers
```bibtex
@inproceedings{reimers-2019-sentence-bert,
    title = "Sentence-BERT: Sentence Embeddings using Siamese BERT-Networks",
    author = "Reimers, Nils and Gurevych, Iryna",
    booktitle = "Proceedings of the 2019 Conference on Empirical Methods in Natural Language Processing",
    month = "11",
    year = "2019",
    publisher = "Association for Computational Linguistics",
    url = "https://arxiv.org/abs/1908.10084",
}
```

<!--
## Glossary

*Clearly define terms in order to be accessible across audiences.*
-->

<!--
## Model Card Authors

*Lists the people who create the model card, providing recognition and accountability for the detailed work that goes into its construction.*
-->

<!--
## Model Card Contact

*Provides a way for people who have updates to the Model Card, suggestions, or questions, to contact the Model Card authors.*
-->
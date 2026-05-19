update system_configs
set config_value = 'jpg,jpeg,jfif,png,webp,gif,bmp',
    updated_at = now()
where config_key = 'upload.image_extensions'
  and regexp_replace(lower(config_value), '\s+', '', 'g') = 'jpg,jpeg,png,webp,gif';

-- Production reset helper. Do not put this file into Flyway migrations.
--
-- Keeps:
--   - admin_users
--   - admin_roles
--   - admin_permissions
--   - admin_user_roles
--   - admin_role_permissions
--   - system_configs
--
-- Clears:
--   - student accounts and preferences
--   - learning records, favorites, statistics, attempts and leaderboards
--   - content data and content assignment data
--   - media metadata
--   - classes and memberships
--   - payment/member business data
--   - admin operation logs

begin;

truncate table
  admin_operation_logs,
  payment_notifications,
  user_memberships,
  payment_orders,
  membership_plans,
  exercise_attempts,
  user_sentence_favorites,
  user_sentence_progress,
  user_vocab_favorites,
  user_vocab_item_progress,
  user_vocab_progress,
  matching_game_sessions,
  speech_records,
  asr_jobs,
  study_events,
  user_daily_stats,
  user_learning_summary,
  class_members,
  classes,
  sentence_word_options,
  exercise_set_items,
  sentence_exercises,
  exercise_sets,
  dialogue_line_vocab,
  dialogue_lines,
  video_materials,
  vocab_item_stroke_assets,
  vocab_list_items,
  vocab_items,
  vocab_lists,
  content_translations,
  user_preferences,
  users,
  leaderboard_entries,
  media_assets
restart identity cascade;

commit;

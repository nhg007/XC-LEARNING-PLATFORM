export default {
  app: {
    title: 'Chinese Learning'
  },
  language: {
    label: 'Interface language',
    zhCN: '简体中文',
    en: 'English',
    ru: 'Русский'
  },
  common: {
    refresh: 'Refresh',
    logout: 'Sign out',
    back: 'Back',
    available: 'Available',
    memberRequired: 'Member',
    comingSoon: 'Coming soon',
    russian: 'Russian',
    english: 'English',
    basic: 'Basic',
    confirm: 'Please confirm',
    userFallback: 'User {id}'
  },
  units: {
    zeroDays: '0 days',
    days: '{count} days',
    durationHoursMinutes: '{hours}h {minutes}m',
    durationMinutes: '{minutes}m'
  },
  api: {
    baseUrlMissing: 'VITE_API_BASE_URL is not configured',
    requestFailed: 'Request failed'
  },
  login: {
    mark: '学',
    eyebrow: 'A student entrance for learning Chinese',
    title: 'Chinese Learning',
    subtitle: 'Practice a little every day and build real language muscle.',
    panelTitle: 'Start today\'s lesson',
    panelSubtitle: 'Sign in with your account, or create one for a 7-day trial.',
    loginTab: 'Sign in',
    registerTab: 'Sign up',
    email: 'Email',
    emailPlaceholder: "student{'@'}example.com",
    nickname: 'Nickname',
    nicknamePlaceholder: 'Your learner name',
    password: 'Password',
    passwordPlaceholder: 'Enter your password',
    enter: 'Start learning',
    registerStart: 'Start 7-day trial',
    trialNote: 'Trial access includes vocabulary, sentence practice, classes, and learning records.',
    previewLesson: 'Today\'s practice',
    previewLevel: 'HSK Basics',
    previewHanzi: '你好',
    previewPinyin: 'nǐ hǎo',
    previewMeaning: 'Hello',
    statStreak: 'Streak',
    statStreakValue: '7 days',
    statAccuracy: 'Accuracy',
    statAccuracyValue: '92%',
    statWords: 'Words learned',
    statWordsValue: '180',
    benefitListen: 'Practice audio, pinyin, characters, and meanings together',
    benefitPractice: 'Sentence practice and vocabulary records sync automatically',
    benefitClass: 'Classes, teacher review, and learning stats are supported',
    validEmail: 'Please enter a valid email',
    passwordRequired: 'Please enter your password',
    passwordLength: 'Password must be 8-72 characters',
    nicknameLength: 'Nickname can be up to 100 characters',
    loginSuccess: 'Signed in',
    trialStarted: 'Your 7-day trial has started'
  },
  home: {
    welcome: '{name}, keep going today.',
    learner: 'Learner',
    accessStatus: 'Access',
    remainingTime: 'Remaining',
    currentStreak: 'Streak',
    accuracy: 'Accuracy',
    vocabLists: 'Vocabulary lists',
    myClasses: 'My classes',
    emptyVocabLists: 'No vocabulary lists',
    emptyClasses: 'No classes',
    table: {
      name: 'Name',
      level: 'Level',
      type: 'Type',
      className: 'Class',
      role: 'Role',
      inviteCode: 'Invite code'
    },
    access: {
      member: 'Member',
      trial: 'Trial',
      free: 'Free'
    },
    featureLocked: 'This feature requires trial or membership access',
    features: {
      vocab: {
        title: 'Vocabulary',
        description: 'HSK, YCT, themed lists, and favorites.'
      },
      practice: {
        title: 'Sentence practice',
        description: 'Audio ordering, dictation, and pinyin prompts.'
      },
      dialogue: {
        title: 'Dialogue training',
        description: 'Listen, repeat, record, and review local ASR feedback.'
      },
      matching: {
        title: 'Matching game',
        description: 'Match Chinese words with English or Russian meanings.'
      },
      classroom: {
        title: 'Classes',
        description: 'View class members and learning stats.'
      },
      records: {
        title: 'Learning records',
        description: 'Streaks, accuracy, progress, and rankings.'
      }
    }
  },
  vocab: {
    title: 'Vocabulary',
    empty: 'No vocabulary',
    showPinyin: 'Show pinyin',
    hidePinyin: 'Hide pinyin',
    showMeaning: 'Show meaning',
    showHanzi: 'Show Chinese',
    favorite: 'Favorite',
    unfavorite: 'Unfavorite',
    playAudio: 'Play audio',
    previous: 'Previous',
    next: 'Next',
    favorited: 'Added to favorites',
    unfavorited: 'Removed from favorites'
  },
  practice: {
    title: 'Sentence practice',
    selectSet: 'Choose an exercise set',
    noSets: 'No exercise sets',
    playAudio: 'Play audio',
    answerPlaceholder: 'Enter your Chinese answer',
    standardAnswer: 'Standard answer: {answer}',
    submit: 'Submit',
    showAnswer: 'Show answer',
    previous: 'Previous',
    next: 'Next',
    changeSet: 'Change set',
    selectWordsWarning: 'Please select word blocks',
    answerRequired: 'Please enter an answer',
    types: {
      audio_order: 'Audio ordering',
      audio_dictation: 'Audio dictation',
      pinyin_dictation: 'Pinyin dictation',
      translation_order: 'Translation ordering'
    }
  },
  records: {
    title: 'Learning records',
    subtitle: 'Review recent activity, daily stats, and overall progress.',
    totalStudyTime: 'Total study time',
    exerciseCount: 'Exercises',
    accuracy: 'Accuracy',
    longestStreak: 'Longest streak',
    dailyStats: 'Daily stats',
    events: 'Learning activity',
    allTypes: 'All types',
    emptyDailyStats: 'No daily stats',
    emptyEvents: 'No learning activity',
    reports: {
      studyTrend: 'Study time trend',
      accuracyTrend: 'Accuracy trend',
      activityMix: 'Practice mix',
      recentDays: 'Last {count} days',
      totalItems: 'Total {count}',
      noData: 'No report data'
    },
    table: {
      date: 'Date',
      studyTime: 'Study time',
      exercises: 'Exercises',
      correct: 'Correct',
      vocab: 'Vocabulary',
      accuracy: 'Accuracy',
      type: 'Type',
      result: 'Result',
      duration: 'Duration',
      time: 'Time'
    },
    eventTypes: {
      exercise: 'Exercise',
      vocab: 'Vocabulary',
      dialogue: 'Dialogue',
      matching_game: 'Matching game'
    },
    results: {
      correct: 'Correct',
      wrong: 'Wrong',
      completed: 'Completed'
    }
  },
  classroom: {
    title: 'Classes',
    subtitle: 'Create or join classes and view member learning stats.',
    createName: 'Class name',
    createDescription: 'Class description',
    create: 'Create',
    inviteCode: 'Invite code',
    join: 'Join',
    emptyRooms: 'No classes',
    chooseRoom: 'Choose a class',
    noDescription: 'No description',
    activeMembers: 'Active members',
    pendingMembers: 'Pending',
    myRole: 'My role',
    inactiveAlert: 'Your member status is not active yet, so member and stats data are unavailable.',
    addByEmail: 'Add by email',
    addByUserId: 'Or user ID',
    addMember: 'Add / request',
    members: 'Members',
    stats: 'Learning stats',
    table: {
      member: 'Member',
      role: 'Role',
      status: 'Status',
      actions: 'Actions',
      studyTime: 'Study time',
      exercises: 'Exercises',
      correct: 'Correct',
      accuracy: 'Accuracy',
      vocab: 'Vocabulary',
      lastStudy: 'Last study'
    },
    actions: {
      approve: 'Approve',
      reject: 'Reject',
      remove: 'Remove'
    },
    notifications: {
      nameRequired: 'Please enter a class name',
      created: 'Class created',
      inviteRequired: 'Please enter an invite code',
      joined: 'Joined class',
      memberRequired: 'Please enter email or user ID',
      memberHandled: 'Member handled',
      approved: 'Approved',
      rejected: 'Rejected',
      removed: 'Removed'
    },
    confirmRemoveTitle: 'Remove member',
    confirmRemoveMessage: 'Remove this member?',
    roles: {
      teacher: 'Teacher',
      member: 'Member'
    },
    statuses: {
      active: 'Active',
      pending_teacher_review: 'Pending review',
      invited: 'Invited',
      rejected: 'Rejected',
      left: 'Left',
      removed: 'Removed'
    }
  }
}

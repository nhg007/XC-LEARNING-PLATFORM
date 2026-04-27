export default {
  app: {
    title: '汉语学习网站'
  },
  language: {
    label: '界面语言',
    zhCN: '简体中文',
    en: 'English',
    ru: 'Русский'
  },
  common: {
    refresh: '刷新',
    logout: '退出',
    back: '返回',
    available: '可用',
    memberRequired: '需会员',
    comingSoon: '功能建设中',
    russian: '俄语',
    english: '英语',
    basic: '基础',
    confirm: '请确认',
    userFallback: '用户 {id}'
  },
  units: {
    zeroDays: '0 天',
    days: '{count} 天',
    durationHoursMinutes: '{hours}小时{minutes}分',
    durationMinutes: '{minutes}分'
  },
  api: {
    baseUrlMissing: 'VITE_API_BASE_URL 未配置',
    requestFailed: '请求失败'
  },
  login: {
    mark: '学',
    eyebrow: '为学生设计的中文学习入口',
    title: '汉语学习',
    subtitle: '每天一点点，把听、说、读、写练成肌肉记忆。',
    panelTitle: '开始今天的学习',
    panelSubtitle: '登录已有账号，或注册后进入 7 天试用。',
    loginTab: '登录',
    registerTab: '注册',
    email: '邮箱',
    emailPlaceholder: "student{'@'}example.com",
    nickname: '昵称',
    nicknamePlaceholder: '你的学习昵称',
    password: '密码',
    passwordPlaceholder: '请输入密码',
    enter: '进入学习',
    registerStart: '注册并开始试用',
    trialNote: '注册后可体验词汇、句子练习、班级与学习记录。',
    previewLesson: '今日练习',
    previewLevel: 'HSK 基础',
    previewHanzi: '你好',
    previewPinyin: 'nǐ hǎo',
    previewMeaning: '你好 / Hello',
    statStreak: '连续学习',
    statStreakValue: '7 天',
    statAccuracy: '正确率',
    statAccuracyValue: '92%',
    statWords: '已背词',
    statWordsValue: '180',
    benefitListen: '听音、拼音、汉字和释义一起练',
    benefitPractice: '句子练习和背词记录自动同步',
    benefitClass: '支持班级、老师审核和学习统计',
    validEmail: '请输入有效邮箱',
    passwordRequired: '请输入密码',
    passwordLength: '密码长度需为 8-72 个字符',
    nicknameLength: '昵称最多 100 个字符',
    loginSuccess: '登录成功',
    trialStarted: '已开启 7 天试用'
  },
  home: {
    welcome: '{name}，今天继续练习。',
    learner: '学习者',
    accessStatus: '权限状态',
    remainingTime: '剩余时间',
    currentStreak: '连续学习',
    accuracy: '总正确率',
    vocabLists: '词汇表',
    myClasses: '我的班级',
    emptyVocabLists: '暂无词汇表',
    emptyClasses: '暂无班级',
    table: {
      name: '名称',
      level: '级别',
      type: '类型',
      className: '班级',
      role: '角色',
      inviteCode: '邀请码'
    },
    access: {
      member: '会员',
      trial: '试用中',
      free: '免费'
    },
    featureLocked: '当前功能需要试用或会员权限',
    features: {
      vocab: {
        title: '背单词',
        description: 'HSK、YCT、分类词汇和收藏夹。'
      },
      practice: {
        title: '句子练习',
        description: '听录音排序、听写、拼音写句子。'
      },
      dialogue: {
        title: '台词训练',
        description: '听台词、跟读录音、本地 ASR 识别。'
      },
      matching: {
        title: '连连看',
        description: '汉字与英/俄释义匹配游戏。'
      },
      classroom: {
        title: '班级',
        description: '查看班级成员和学习统计。'
      },
      records: {
        title: '学习记录',
        description: '连续天数、正确率和排行榜。'
      }
    }
  },
  vocab: {
    title: '背单词',
    empty: '暂无词汇',
    showPinyin: '显示拼音',
    hidePinyin: '隐藏拼音',
    showMeaning: '看释义',
    showHanzi: '看汉字',
    favorite: '收藏',
    unfavorite: '取消收藏',
    playAudio: '播放读音',
    previous: '上一词',
    next: '下一词',
    favorited: '已收藏',
    unfavorited: '已取消收藏'
  },
  practice: {
    title: '句子练习',
    selectSet: '选择题组开始练习',
    noSets: '暂无题组',
    playAudio: '播放录音',
    answerPlaceholder: '请输入汉语答案',
    standardAnswer: '标准答案：{answer}',
    submit: '提交答案',
    showAnswer: '查看答案',
    previous: '上一题',
    next: '下一题',
    changeSet: '切换题组',
    selectWordsWarning: '请选择词块',
    answerRequired: '请输入答案',
    types: {
      audio_order: '听音排序',
      audio_dictation: '听写',
      pinyin_dictation: '拼音写句',
      translation_order: '译文排序'
    }
  },
  records: {
    title: '学习记录',
    subtitle: '查看最近学习行为、每日统计和总体进度。',
    totalStudyTime: '总学习时长',
    exerciseCount: '练习次数',
    accuracy: '总正确率',
    longestStreak: '最长连续',
    dailyStats: '每日统计',
    events: '学习行为',
    allTypes: '全部类型',
    emptyDailyStats: '暂无每日统计',
    emptyEvents: '暂无学习行为',
    table: {
      date: '日期',
      studyTime: '学习时长',
      exercises: '做题',
      correct: '正确',
      vocab: '背词',
      accuracy: '正确率',
      type: '类型',
      result: '结果',
      duration: '时长',
      time: '时间'
    },
    eventTypes: {
      exercise: '练习',
      vocab: '背词',
      dialogue: '台词',
      matching_game: '连连看'
    },
    results: {
      correct: '正确',
      wrong: '错误',
      completed: '完成'
    }
  },
  classroom: {
    title: '班级',
    subtitle: '创建班级、加入班级，查看成员学习统计。',
    createName: '班级名称',
    createDescription: '班级说明',
    create: '创建',
    inviteCode: '邀请码',
    join: '加入',
    emptyRooms: '暂无班级',
    chooseRoom: '请选择班级',
    noDescription: '暂无说明',
    activeMembers: '正式成员',
    pendingMembers: '待审核',
    myRole: '我的角色',
    inactiveAlert: '你的成员状态还不是正式成员，暂不能查看成员和统计。',
    addByEmail: '按邮箱添加成员',
    addByUserId: '或用户 ID',
    addMember: '添加/提交申请',
    members: '成员',
    stats: '学习统计',
    table: {
      member: '成员',
      role: '角色',
      status: '状态',
      actions: '操作',
      studyTime: '学习时长',
      exercises: '做题',
      correct: '正确',
      accuracy: '正确率',
      vocab: '背词',
      lastStudy: '最近学习'
    },
    actions: {
      approve: '通过',
      reject: '拒绝',
      remove: '移除'
    },
    notifications: {
      nameRequired: '请输入班级名称',
      created: '班级已创建',
      inviteRequired: '请输入邀请码',
      joined: '已加入班级',
      memberRequired: '请输入邮箱或用户 ID',
      memberHandled: '成员已处理',
      approved: '已通过',
      rejected: '已拒绝',
      removed: '已移除'
    },
    confirmRemoveTitle: '移除成员',
    confirmRemoveMessage: '确认移除该成员？',
    roles: {
      teacher: '老师',
      member: '成员'
    },
    statuses: {
      active: '正式',
      pending_teacher_review: '待审核',
      invited: '已邀请',
      rejected: '已拒绝',
      left: '已退出',
      removed: '已移除'
    }
  }
}

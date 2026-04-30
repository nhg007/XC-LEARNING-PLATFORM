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
    membership: '会员',
    referenceLanguage: '参考语言',
    confirm: '请确认',
    yes: '是',
    no: '否',
    userFallback: '用户 {id}',
    studyLanguages: {
      zh: '汉语',
      ru: '俄语',
      en: '英语'
    }
  },
  units: {
    zeroDays: '0 天',
    days: '{count} 天',
    durationHoursMinutes: '{hours}小时{minutes}分',
    durationMinutes: '{minutes}分'
  },
  api: {
    baseUrlMissing: 'VITE_API_BASE_URL 未配置',
    requestFailed: '请求失败',
    membershipRequired: '当前功能需要试用或会员权限'
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
    benefitClass: '支持加入班级和查看班级成员',
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
      favorites: {
        title: '收藏夹',
        description: '查看已收藏单词，快速回到原词表复习。'
      },
      practice: {
        title: '句子练习',
        description: '听录音排序、听写、拼音写句子。'
      },
      dialogue: {
        title: '台词训练',
        description: '台词排序、听写和跟读练习。'
      },
      matching: {
        title: '连连看',
        description: '目标语内容与参考语释义匹配游戏。'
      },
      elimination: {
        title: '消消乐',
        description: '中文和参考语释义混排成卡片，点对即可消除。'
      },
      classroom: {
        title: '班级',
        description: '加入班级并查看成员信息。'
      },
      records: {
        title: '学习记录',
        description: '连续天数、正确率和排行榜。'
      }
    }
  },
  vocab: {
    title: '背单词',
    favorites: '收藏夹',
    favoritesTitle: '收藏夹',
    favoritesSubtitle: '集中查看已收藏的单词，适合考前复盘和连连看练习。',
    favoriteCount: '共 {count} 个收藏词',
    emptyFavorites: '暂无收藏词，背单词时点击收藏后会显示在这里。',
    browseLists: '浏览词表',
    openList: '进入词表',
    empty: '暂无词汇',
    showPinyin: '显示拼音',
    hidePinyin: '隐藏拼音',
    showMeaning: '看释义',
    showHanzi: '看汉字',
    favorite: '收藏',
    unfavorite: '取消收藏',
    playPronunciation: '播放读音',
    playAudio: '播放释义',
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
  dialogue: {
    title: '台词训练',
    subtitle: '选择材料，按台词顺序练听写、排序和跟读。',
    noMaterials: '暂无台词材料',
    noLines: '暂无台词',
    noDescription: '暂无说明',
    noTranslation: '暂无译文',
    noVocabAnalysis: '暂无词汇解析',
    lineCount: '{count} 句台词',
    changeMaterial: '切换材料',
    currentLine: '第 {current} / {total} 句',
    translationPrompt: '译文提示',
    playAudio: '播放台词',
    audioPending: '暂无音频',
    answerPlaceholder: '请输入听到的汉语台词',
    selectWordsHint: '点击下方字词组成台词',
    selectWordsWarning: '请选择字词',
    answerRequired: '请输入台词',
    submit: '提交',
    showAnswer: '查看答案',
    standardAnswer: '标准答案：{answer}',
    previous: '上一句',
    next: '下一句',
    completeShadow: '完成跟读',
    shadowHint: '先听或看台词，再跟读一遍。ASR 评分后续接入。',
    analysis: '答案与解析',
    analysisHint: '提交或查看答案后展示台词、译文和词汇解析。',
    types: {
      all: '全部',
      drama: '电视剧',
      short_video: '短视频',
      cartoon: '动画'
    },
    modes: {
      order: '排序',
      dictation: '听写',
      shadow: '跟读'
    }
  },
  matching: {
    title: '连连看',
    subtitle: '选择词汇来源、参考语言和难度，匹配{target}内容与{reference}释义。',
    playingSubtitle: '点击左侧{target}内容和右侧{reference}释义，找出正确配对。',
    newGame: '新游戏',
    settings: '游戏设置',
    source: '词汇来源',
    vocabList: '词汇表',
    sound: '音效',
    start: '开始游戏',
    rulesTitle: '规则',
    rulePairs: '配对',
    rulePairsText: '每个{target}词句只对应一个{reference}释义，选错会累计错误次数。',
    ruleStats: '记录',
    ruleStatsText: '全部配对后会保存学习记录，并进入学习统计。',
    progress: '进度',
    errors: '错误',
    elapsed: '用时',
    difficulty: '难度',
    targetColumn: '{language}内容',
    referenceColumn: '{language}释义',
    completedTitle: '完成',
    completedText: '用时 {time}，错误 {errors} 次。',
    completedToast: '连连看已完成',
    selectListWarning: '请选择词汇表',
    selectStageWarning: '请选择关卡',
    saveFailed: '保存记录失败',
    sources: {
      vocab_list: '词汇表',
      favorites: '收藏夹'
    },
    difficulties: {
      '4x4': '4 对',
      '7x7': '7 对',
      '10x10': '10 对'
    }
  },
  elimination: {
    title: '消消乐',
    subtitle: '选择词汇来源和关卡，把汉字与参考语言释义成对消除。',
    playerBest: '最佳成绩',
    points: '积分',
    setup: '选择关卡',
    setupDesc: '先选择词汇来源和大关，再选择要挑战的小关。每关可配置不同卡牌数量。',
    stageGroups: '大关',
    levels: '关卡',
    levelNumber: '第 {number} 关',
    levelCount: '{count} 个小关',
    cards: '{count} 张卡片',
    limit: '限时 {time}',
    remaining: '剩余时间',
    locked: '未解锁',
    lockedWarning: '请先完成上一关',
    bestTime: '最佳 {time}',
    cleared: '已消除',
    combo: '连击',
    boardHint: '每次选择两张卡片。汉字和对应释义配对成功后会消除。',
    completedTitle: '闯关完成',
    completedSummary: '完成 {pairs} 组配对，用时 {time}，错误 {wrong} 次。',
    failedTitle: '时间到',
    failedSummary: '本次完成 {pairs}/{total} 组配对，复习一下再挑战。'
  },
  membership: {
    title: '会员',
    heroTitle: '把练习、听说和复盘完整解锁',
    subtitle: '选择适合自己的学习节奏，开通后即可使用完整练习、台词训练、连连看和学习记录。',
    currentAccess: '当前权限',
    accessLevel: '权限状态',
    remaining: '剩余时间',
    trialEndsAt: '试用结束',
    membershipEndsAt: '会员结束',
    plans: '会员套餐',
    plansHint: '首期支持微信支付和支付宝；开发环境可使用模拟支付完成闭环验证。',
    emptyPlans: '暂无可购买套餐',
    bestValue: '推荐',
    fullAccessBenefit: '解锁完整学习权益',
    validFor: '{count} 天有效期',
    payWith: '{provider}',
    checkoutLabel: '开通进度',
    checkoutEmptyTitle: '先选择一个套餐',
    durationDays: '{count} 天权益',
    orderTitle: '支付订单',
    noOrder: '选择套餐和支付方式后，这里会显示订单和支付状态。',
    orderNo: '订单号',
    orderPlan: '套餐',
    orderAmount: '金额',
    orderProvider: '支付方式',
    orderStatus: '订单状态',
    paymentLink: '支付链接',
    mockNotice: '当前为开发环境模拟支付，不会产生真实扣款。',
    simulatePaid: '模拟支付成功',
    refreshOrder: '刷新订单',
    createSuccess: '订单已创建',
    paidSuccess: '会员已开通',
    duration: {
      day: '{count} 天',
      month: '{count} 个月',
      custom: '{days} 天'
    },
    providers: {
      wechat_pay: '微信支付',
      alipay: '支付宝'
    },
    benefits: {
      practice: '句子练习',
      dialogue: '台词训练',
      records: '学习复盘',
      classroom: '班级学习'
    },
    orderStatuses: {
      pending: '待支付',
      paid: '已支付',
      failed: '失败',
      refunded: '已退款'
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
    attempts: '作答明细',
    leaderboards: '排行榜',
    leaderboardPeriod: '周期',
    leaderboardMetric: '指标',
    leaderboardRefresh: '刷新榜单',
    allTypes: '全部类型',
    emptyDailyStats: '暂无每日统计',
    emptyEvents: '暂无学习行为',
    emptyAttempts: '暂无作答明细',
    emptyLeaderboards: '暂无排行榜数据',
    reports: {
      studyTrend: '学习时长趋势',
      accuracyTrend: '正确率趋势',
      activityMix: '练习结构',
      recentDays: '近 {count} 天',
      totalItems: '合计 {count}',
      noData: '暂无报表数据'
    },
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
      time: '时间',
      answer: '作答',
      referenceLanguage: '参考语言',
      showedAnswer: '看过答案',
      rank: '排名',
      user: '学习者',
      metric: '指标',
      score: '分数',
      period: '周期',
      generatedAt: '生成时间'
    },
    eventTypes: {
      exercise: '练习',
      vocab: '背词',
      dialogue: '台词',
      matching_game: '连连看'
    },
    leaderboardPeriods: {
      daily: '日榜',
      weekly: '周榜',
      monthly: '月榜',
      all: '总榜'
    },
    leaderboardMetrics: {
      streak: '连续学习',
      accuracy: '正确率',
      vocab_count: '背词数',
      game_score: '游戏分'
    },
    results: {
      correct: '正确',
      wrong: '错误',
      completed: '完成'
    }
  },
  classroom: {
    title: '班级',
    subtitle: '加入班级，查看自己的班级和成员信息。',
    joinTitle: '加入班级',
    inviteCode: '邀请码',
    join: '加入',
    emptyRooms: '暂无班级',
    chooseRoom: '请选择班级',
    noDescription: '暂无说明',
    activeMembers: '正式成员',
    pendingMembers: '待审核',
    myRole: '我的角色',
    inactiveAlert: '你的成员状态还不是正式成员，暂不能查看成员信息。',
    members: '成员',
    table: {
      member: '成员',
      role: '角色',
      status: '状态'
    },
    notifications: {
      inviteRequired: '请输入邀请码',
      joined: '已加入班级'
    },
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

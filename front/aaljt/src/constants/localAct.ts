export const LOCAL_ACT_CATEGORIES = [
  { code: 'sport', label: '运动健康' },
  { code: 'kids', label: '亲子活动' },
  { code: 'skill', label: '技能课堂' },
  { code: 'market', label: '市集交换' },
  { code: 'eco', label: '环保共建' },
  { code: 'support', label: '邻里互助' },
  { code: 'volunteer', label: '志愿服务' },
  { code: 'welfare', label: '公益助老' }
] as const;

export const LOCAL_ACT_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  REVIEWING: '待审核',
  PENDING_REVIEW: '待审核',
  PUBLISHED: '报名中',
  FULL: '已满员',
  CLOSED: '已截止',
  ONGOING: '进行中',
  FINISHED: '已结束',
  CANCELLED: '已取消'
};

export const ENROLLMENT_STATUS_LABELS: Record<string, string> = {
  confirmed: '已确认',
  pending: '待审核',
  waitlist: '候补中',
  cancelled: '已取消',
  checked_in: '已签到',
  completed: '已完成'
};

export const getCategoryLabel = (code?: string) => {
  if (!code) return '社区活动';
  return LOCAL_ACT_CATEGORIES.find((item) => item.code === code)?.label ?? code;
};

export const getActivityStatusLabel = (status?: string) => {
  if (!status) return '待确认';
  return LOCAL_ACT_STATUS_LABELS[status] ?? status;
};

export const getEnrollmentStatusLabel = (status?: string) => {
  if (!status) return '';
  return ENROLLMENT_STATUS_LABELS[status] ?? status;
};

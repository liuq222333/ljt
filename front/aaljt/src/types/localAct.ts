export type LocalActStatus =
  | 'DRAFT'
  | 'REVIEWING'
  | 'PENDING_REVIEW'
  | 'PUBLISHED'
  | 'FULL'
  | 'CLOSED'
  | 'ONGOING'
  | 'FINISHED'
  | 'CANCELLED'
  | string;

export type LocalActCategory =
  | 'sport'
  | 'kids'
  | 'skill'
  | 'market'
  | 'eco'
  | 'support'
  | 'volunteer'
  | 'welfare'
  | string;

export type EnrollmentStatus =
  | 'confirmed'
  | 'pending'
  | 'waitlist'
  | 'cancelled'
  | 'checked_in'
  | 'completed'
  | string;

export type ApiResp<T> = {
  code: number;
  message?: string;
  data: T;
};

export type LocalActivityDetail = {
  id: number;
  organizerUserId?: number;
  organizer?: string;
  title?: string;
  subtitle?: string;
  category?: LocalActCategory;
  categoryCode?: LocalActCategory;
  description?: string;
  location?: string;
  locationText?: string;
  latitude?: number;
  longitude?: number;
  address?: string;
  coverUrl?: string;
  capacity?: number;
  reserved?: number;
  feeType?: string;
  feeAmount?: number;
  allowWaitlist?: boolean;
  requireCheckin?: boolean;
  status?: LocalActStatus;
  startAt?: string;
  endAt?: string;
  reminderMinutes?: number;
  reviewNote?: string;
  tags?: string[];
  enrollmentStatus?: EnrollmentStatus;
  favorited?: boolean;
};

export type LocalActivityListItem = LocalActivityDetail & {
  distanceKm?: number;
};

export type LocalActEnrollmentActionResponse = {
  enrollmentId?: number;
  status: EnrollmentStatus;
  waitlistRank?: number | null;
};

export type LocalActCreateResponse = {
  activityId?: number;
  id?: number;
  status?: LocalActStatus;
};

export type LocalActMediaUploadResponse = {
  objectKey: string;
  url?: string;
  publicUrl?: string;
  contentType?: string;
  size?: number;
};

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/common/Layout';
import ErrorBoundary from './components/common/ErrorBoundary';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import VerifyEmailPage from './pages/auth/VerifyEmailPage';

// Job Pages
import JobSearchPage from './pages/job/JobSearchPage';
import JobDetailPage from './pages/job/JobDetailPage';
import EmployerJobListPage from './pages/job/EmployerJobListPage';
import JobFormPage from './pages/job/JobFormPage';
import JobApprovalPage from './pages/job/JobApprovalPage';

// ATS Kanban Page
import AtsBoardPage from './pages/ats/AtsBoardPage';

// Interview Pages
import InterviewListPage from './pages/interview/InterviewListPage';
import InterviewSchedulePage from './pages/interview/InterviewSchedulePage';

// Offer Pages
import OfferListPage from './pages/offer/OfferListPage';
import OfferCreatePage from './pages/offer/OfferCreatePage';

// Candidate Pages
import CandidateProfilePage from './pages/candidate/CandidateProfilePage';
import CandidateCvPage from './pages/candidate/CandidateCvPage';
import CandidateApplicationsPage from './pages/candidate/CandidateApplicationsPage';

// Company & Notification Pages
import CompanyProfilePage from './pages/company/CompanyProfilePage';
import NotificationCenterPage from './pages/notification/NotificationCenterPage';

function ProtectedRoute({ children, roles }) {
  const { user, role, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(role)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <ErrorBoundary>
        <BrowserRouter>
          <Layout>
            <Routes>
              {/* Public & Auth */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/verify-email" element={<VerifyEmailPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />

              {/* Job Search & Details */}
              <Route path="/" element={<JobSearchPage />} />
              <Route path="/jobs/:id" element={<JobDetailPage />} />

              {/* Employer / ATS */}
              <Route
                path="/employer/jobs"
                element={
                  <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER', 'SYS_ADMIN']}>
                    <EmployerJobListPage />
                  </ProtectedRoute>
                }
              />
            <Route
              path="/employer/jobs/new"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <JobFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/employer/jobs/:id/edit"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <JobFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/employer/jobs/:jobId/ats"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <AtsBoardPage />
                </ProtectedRoute>
              }
            />

            {/* Admin Job Approvals */}
            <Route
              path="/admin/jobs/approval"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'SYS_ADMIN']}>
                  <JobApprovalPage />
                </ProtectedRoute>
              }
            />

            {/* Interviews */}
            <Route
              path="/employer/interviews"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <InterviewListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/employer/interviews/schedule"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <InterviewSchedulePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/candidate/interviews"
              element={
                <ProtectedRoute roles={['CANDIDATE']}>
                  <InterviewListPage />
                </ProtectedRoute>
              }
            />

            {/* Offers */}
            <Route
              path="/employer/offers"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <OfferListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/employer/offers/new"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <OfferCreatePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/candidate/offers"
              element={
                <ProtectedRoute roles={['CANDIDATE']}>
                  <OfferListPage />
                </ProtectedRoute>
              }
            />

            {/* Candidate Portal */}
            <Route
              path="/candidate/profile"
              element={
                <ProtectedRoute roles={['CANDIDATE']}>
                  <CandidateProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/candidate/cvs"
              element={
                <ProtectedRoute roles={['CANDIDATE']}>
                  <CandidateCvPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/candidate/applications"
              element={
                <ProtectedRoute roles={['CANDIDATE']}>
                  <CandidateApplicationsPage />
                </ProtectedRoute>
              }
            />

            {/* Company & Notifications */}
            <Route
              path="/company/profile"
              element={
                <ProtectedRoute roles={['HR_ADMIN', 'RECRUITER']}>
                  <CompanyProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/notifications"
              element={
                <ProtectedRoute>
                  <NotificationCenterPage />
                </ProtectedRoute>
              }
            />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </ErrorBoundary>
  </AuthProvider>
);
}

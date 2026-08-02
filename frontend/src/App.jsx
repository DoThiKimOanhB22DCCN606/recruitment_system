import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/common/Layout';

import JobSearchPage from './pages/job/JobSearchPage';
import JobDetailPage from './pages/job/JobDetailPage';
import EmployerJobListPage from './pages/job/EmployerJobListPage';
import JobFormPage from './pages/job/JobFormPage';
import JobApprovalPage from './pages/job/JobApprovalPage';

import CandidateProfilePage from './pages/candidate/CandidateProfilePage';
import CandidateCvPage from './pages/candidate/CandidateCvPage';

import CompanyProfilePage from './pages/company/CompanyProfilePage';

export default function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          {/* Job module */}
          <Route path="/" element={<JobSearchPage />} />
          <Route path="/jobs/:id" element={<JobDetailPage />} />
          <Route path="/employer/jobs" element={<EmployerJobListPage />} />
          <Route path="/employer/jobs/new" element={<JobFormPage />} />
          <Route path="/employer/jobs/:id/edit" element={<JobFormPage />} />
          <Route path="/admin/jobs/approval" element={<JobApprovalPage />} />

          {/* Candidate module */}
          <Route path="/candidate/profile" element={<CandidateProfilePage />} />
          <Route path="/candidate/cvs" element={<CandidateCvPage />} />

          {/* Company module */}
          <Route path="/company/profile" element={<CompanyProfilePage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}

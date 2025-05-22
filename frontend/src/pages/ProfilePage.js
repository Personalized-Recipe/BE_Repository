import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { userAPI } from '../services/api';
import './ProfilePage.css';

const ProfilePage = () => {
  const navigate = useNavigate();
  const [userData, setUserData] = useState({
    username: '',
    name: '',
    age: '',
    gender: '',
    preferences: '',
    isPregnant: false,
    healthConditions: '',
    allergies: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const userId = localStorage.getItem('userId');

  useEffect(() => {
    if (!userId) {
      navigate('/login');
      return;
    }
    
    fetchUserData();
  }, [userId, navigate]);

  const fetchUserData = async () => {
    setLoading(true);
    try {
      const response = await userAPI.getProfile(userId);
      setUserData(response.data);
    } catch (err) {
      setError('프로필 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setUserData({
      ...userData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await userAPI.updateProfile(userId, userData);
      setSuccess('프로필이 성공적으로 업데이트되었습니다.');
    } catch (err) {
      setError(err.response?.data?.message || '프로필 업데이트 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (loading && !userData.username) {
    return <div className="loading">로딩 중...</div>;
  }

  return (
    <div className="profile-page">
      <h2>내 프로필</h2>
      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}
      <form onSubmit={handleSubmit} className="profile-form">
        <div className="form-section">
          <h3>기본 정보</h3>
          <div className="form-group">
            <label htmlFor="username" className="form-label">아이디</label>
            <input
              type="text"
              id="username"
              name="username"
              value={userData.username}
              className="form-control"
              disabled
            />
          </div>
          <div className="form-group">
            <label htmlFor="name" className="form-label">이름</label>
            <input
              type="text"
              id="name"
              name="name"
              value={userData.name}
              onChange={handleChange}
              className="form-control"
              required
            />
          </div>
        </div>

        <div className="form-section">
          <h3>개인 정보</h3>
          <div className="form-group">
            <label htmlFor="age" className="form-label">나이</label>
            <input
              type="number"
              id="age"
              name="age"
              value={userData.age}
              onChange={handleChange}
              className="form-control"
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="gender" className="form-label">성별</label>
            <select
              id="gender"
              name="gender"
              value={userData.gender}
              onChange={handleChange}
              className="form-control"
              required
            >
              <option value="">선택하세요</option>
              <option value="남">남성</option>
              <option value="여">여성</option>
            </select>
          </div>
          {userData.gender === '여' && (
            <div className="form-group">
              <label className="form-checkbox-label">
                <input
                  type="checkbox"
                  name="isPregnant"
                  checked={userData.isPregnant}
                  onChange={handleChange}
                />
                임신 중
              </label>
            </div>
          )}
        </div>

        <div className="form-section">
          <h3>건강 정보</h3>
          <div className="form-group">
            <label htmlFor="preferences" className="form-label">식품 선호도</label>
            <textarea
              id="preferences"
              name="preferences"
              value={userData.preferences}
              onChange={handleChange}
              className="form-control"
              placeholder="선호하는 음식이나 재료를 입력하세요"
            />
          </div>
          <div className="form-group">
            <label htmlFor="healthConditions" className="form-label">건강 상태</label>
            <textarea
              id="healthConditions"
              name="healthConditions"
              value={userData.healthConditions}
              onChange={handleChange}
              className="form-control"
              placeholder="당뇨, 고혈압 등 건강 상태를 입력하세요"
            />
          </div>
          <div className="form-group">
            <label htmlFor="allergies" className="form-label">알러지 정보</label>
            <textarea
              id="allergies"
              name="allergies"
              value={userData.allergies}
              onChange={handleChange}
              className="form-control"
              placeholder="알러지가 있는 음식이나 재료를 입력하세요"
            />
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? '저장 중...' : '저장'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default ProfilePage; 
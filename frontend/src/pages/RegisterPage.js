import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../services/api';
import './RegisterPage.css';

const RegisterPage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    password: '',
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

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await authAPI.register(formData);
      alert('회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || '회원가입 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-page">
      <h2>회원가입</h2>
      {error && <div className="error-message">{error}</div>}
      <form onSubmit={handleSubmit} className="register-form">
        <div className="form-section">
          <h3>기본 정보</h3>
          <div className="form-group">
            <label htmlFor="username" className="form-label">아이디</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              className="form-control"
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="password" className="form-label">비밀번호</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="form-control"
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="name" className="form-label">이름</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
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
              value={formData.age}
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
              value={formData.gender}
              onChange={handleChange}
              className="form-control"
              required
            >
              <option value="">선택하세요</option>
              <option value="남">남성</option>
              <option value="여">여성</option>
            </select>
          </div>
          {formData.gender === '여' && (
            <div className="form-group">
              <label className="form-checkbox-label">
                <input
                  type="checkbox"
                  name="isPregnant"
                  checked={formData.isPregnant}
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
              value={formData.preferences}
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
              value={formData.healthConditions}
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
              value={formData.allergies}
              onChange={handleChange}
              className="form-control"
              placeholder="알러지가 있는 음식이나 재료를 입력하세요"
            />
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? '처리 중...' : '회원가입'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default RegisterPage; 
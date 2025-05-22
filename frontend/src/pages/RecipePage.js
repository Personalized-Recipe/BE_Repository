import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { recipeAPI } from '../services/api';
import './RecipePage.css';

const RecipePage = () => {
  const navigate = useNavigate();
  const [request, setRequest] = useState('');
  const [recipe, setRecipe] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const userId = localStorage.getItem('userId');
  const isLoggedIn = !!userId;

  useEffect(() => {
    if (isLoggedIn) {
      fetchRecipeHistory();
    }
  }, [isLoggedIn]);

  const fetchRecipeHistory = async () => {
    try {
      const response = await recipeAPI.getHistory(userId);
      setHistory(response.data);
    } catch (err) {
      console.error('레시피 기록을 불러오는데 실패했습니다:', err);
    }
  };

  const handleRequestChange = (e) => {
    setRequest(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!request.trim()) return;

    setLoading(true);
    setError('');
    setRecipe(null);

    try {
      const recipeRequest = {
        userId: isLoggedIn ? userId : null,
        request: request
      };

      const response = await recipeAPI.requestRecipe(recipeRequest);
      setRecipe(response.data);
      
      if (isLoggedIn) {
        fetchRecipeHistory();
      }
    } catch (err) {
      setError(err.response?.data?.message || '레시피 생성 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleHistoryClick = (recipe) => {
    setRecipe(recipe);
    setRequest(recipe.request);
  };

  return (
    <div className="recipe-page">
      <h2>레시피 추천</h2>
      
      <div className="recipe-form-container">
        <form onSubmit={handleSubmit} className="recipe-form">
          <div className="form-group">
            <label htmlFor="request" className="form-label">어떤 레시피를 원하시나요?</label>
            <textarea
              id="request"
              value={request}
              onChange={handleRequestChange}
              className="form-control"
              placeholder="예: 오늘 점심으로 기름진 음식을 먹고 싶어요."
              rows="3"
              required
            />
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '레시피 생성 중...' : '레시피 추천 받기'}
            </button>
          </div>
        </form>

        {!isLoggedIn && (
          <div className="login-prompt">
            <p>회원가입을 하시면 더 정확한 레시피 추천과 기록 저장이 가능합니다.</p>
            <button onClick={() => navigate('/register')} className="btn btn-secondary">
              회원가입 하기
            </button>
          </div>
        )}
      </div>

      {error && <div className="error-message">{error}</div>}

      {recipe && (
        <div className="recipe-result">
          <h3>추천 레시피</h3>
          <div className="recipe-content">
            <p className="recipe-request"><strong>요청:</strong> {recipe.request}</p>
            <div className="recipe-response">
              {recipe.response.split('\n').map((line, index) => (
                <p key={index}>{line}</p>
              ))}
            </div>
          </div>
        </div>
      )}

      {isLoggedIn && history.length > 0 && (
        <div className="recipe-history">
          <h3>이전 레시피 기록</h3>
          <div className="history-list">
            {history.map((item) => (
              <div 
                key={item.id} 
                className="history-item"
                onClick={() => handleHistoryClick(item)}
              >
                <p>{item.request}</p>
                <span className="history-date">
                  {new Date(item.createdAt).toLocaleDateString()}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default RecipePage; 
import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Button, TextField, MenuItem, Card, CardContent,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Chip, Stack, Divider, Alert, Grid
} from '@mui/material';
import { Delete as DeleteIcon, CheckCircle as CheckCircleIcon } from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import tipPoolApi from '../api/tipPoolApi';
import achievementApi from '../api/achievementApi';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

const TipPoolPage = () => {
  const navigate = useNavigate();
  const [pools, setPools] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Create Pool State
  const [totalTip, setTotalTip] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [restaurantName, setRestaurantName] = useState('');
  const [distributionType, setDistributionType] = useState('EQUAL');
  const [members, setMembers] = useState([{ name: '', allocationPercentage: '' }, { name: '', allocationPercentage: '' }]);
  const [previewPool, setPreviewPool] = useState(null);
  
  // View Pool State
  const [viewPool, setViewPool] = useState(null);

  useEffect(() => {
    fetchPools();
  }, []);

  const fetchPools = async () => {
    try {
      const data = await tipPoolApi.getPools();
      setPools(data);
    } catch (err) {
      console.error('Failed to load pools', err);
    }
  };

  const handleAddMember = () => {
    if (members.length < 20) {
      setMembers([...members, { name: '', allocationPercentage: '' }]);
    }
  };

  const handleRemoveMember = (index) => {
    if (members.length > 2) {
      const newMembers = [...members];
      newMembers.splice(index, 1);
      setMembers(newMembers);
    }
  };

  const handleMemberChange = (index, field, value) => {
    const newMembers = [...members];
    newMembers[index][field] = value;
    setMembers(newMembers);
  };

  const validateForm = () => {
    setError('');
    if (!totalTip || isNaN(totalTip) || Number(totalTip) <= 0) return 'Total tip must be > 0';
    if (!restaurantName.trim()) return 'Restaurant name is required';
    if (members.length < 2 || members.length > 20) return 'Members must be between 2 and 20';
    
    let sumPercentage = 0;
    const names = new Set();
    
    for (const m of members) {
      if (!m.name.trim()) return 'Member names cannot be blank';
      if (names.has(m.name.trim().toLowerCase())) return `Duplicate member name: ${m.name}`;
      names.add(m.name.trim().toLowerCase());
      
      if (distributionType === 'PERCENTAGE') {
        if (!m.allocationPercentage || isNaN(m.allocationPercentage) || Number(m.allocationPercentage) < 0) {
          return 'Valid percentages are required for all members';
        }
        sumPercentage += Number(m.allocationPercentage);
      }
    }
    
    if (distributionType === 'PERCENTAGE' && Math.abs(sumPercentage - 100) > 0.001) {
      return `Total percentages must equal exactly 100%. Current: ${sumPercentage}%`;
    }
    return null;
  };

  const handlePreview = async () => {
    const err = validateForm();
    if (err) {
      setError(err);
      return;
    }
    
    try {
      setLoading(true);
      const payload = {
        totalTip: Number(totalTip),
        currency,
        restaurantName,
        distributionType,
        members: members.map(m => ({
          name: m.name,
          allocationPercentage: distributionType === 'PERCENTAGE' ? Number(m.allocationPercentage) : null
        }))
      };
      
      const created = await tipPoolApi.createPool(payload);
      setPreviewPool(created);
      fetchPools(); // Refresh list
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to calculate pool');
    } finally {
      setLoading(false);
    }
  };

  const handleFinalize = async (poolId) => {
    try {
      setLoading(true);
      await tipPoolApi.finalizePool(poolId);
      setPreviewPool(null);
      setViewPool(null);
      fetchPools();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to finalize pool');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (poolId) => {
    try {
      setLoading(true);
      await tipPoolApi.deletePool(poolId);
      if (previewPool?.id === poolId) setPreviewPool(null);
      if (viewPool?.id === poolId) setViewPool(null);
      fetchPools();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete pool');
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setPreviewPool(null);
    setViewPool(null);
    setTotalTip('');
    setRestaurantName('');
    setMembers([{ name: '', allocationPercentage: '' }, { name: '', allocationPercentage: '' }]);
    setError('');
  };

  return (
    <Box 
      component={motion.div}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      sx={{ p: { xs: 2, md: 4 }, maxWidth: 1200, mx: 'auto', width: '100%' }}
    >
      <Typography variant="h3" sx={{ mb: 4, color: '#FFFFFF' }}>
        Tip Pooling for Teams
      </Typography>

      <Grid container spacing={4}>
        <Grid item xs={12} md={7}>
          {previewPool || viewPool ? (
            <Card sx={{ mb: 4 }}>
              <CardContent>
                <Typography variant="h5" sx={{ mb: 2, color: '#fff' }}>
                  Pool Details: {(previewPool || viewPool).restaurantName}
                </Typography>
                
                <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                  <Chip label={`Total: ${(previewPool || viewPool).currency} ${(previewPool || viewPool).totalTip.toFixed(2)}`} color="primary" />
                  <Chip label={(previewPool || viewPool).distributionType} color="secondary" />
                  <Chip 
                    label={(previewPool || viewPool).status} 
                    color={(previewPool || viewPool).status === 'FINALIZED' ? 'success' : 'warning'} 
                  />
                </Box>

                <TableContainer component={Paper} sx={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)' }}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ color: '#9AA0A6' }}>Member</TableCell>
                        <TableCell sx={{ color: '#9AA0A6' }}>Percentage</TableCell>
                        <TableCell sx={{ color: '#9AA0A6' }}>Allocated Amount</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {(previewPool || viewPool).members.map((m, i) => (
                        <TableRow key={i}>
                          <TableCell sx={{ color: '#fff' }}>{m.name}</TableCell>
                          <TableCell sx={{ color: '#fff' }}>{m.allocationPercentage.toFixed(2)}%</TableCell>
                          <TableCell sx={{ color: '#fff' }}>{(previewPool || viewPool).currency} {m.allocatedAmount.toFixed(2)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>

                {(previewPool || viewPool).status === 'DRAFT' && (
                  <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
                    <Button 
                      variant="contained" 
                      color="success" 
                      onClick={() => handleFinalize((previewPool || viewPool).id)}
                      disabled={loading}
                      startIcon={<CheckCircleIcon />}
                    >
                      Finalize Pool
                    </Button>
                    <Button 
                      variant="outlined" 
                      color="error" 
                      onClick={() => handleDelete((previewPool || viewPool).id)}
                      disabled={loading}
                      startIcon={<DeleteIcon />}
                    >
                      Discard Draft
                    </Button>
                  </Box>
                )}
                <Button sx={{ mt: 2 }} onClick={resetForm}>Create New Pool</Button>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardContent>
                <Typography variant="h5" sx={{ mb: 3, color: '#fff' }}>Create Tip Pool</Typography>
                
                {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

                <Stack spacing={3}>
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <TextField
                      fullWidth label="Total Tip Amount" type="number"
                      value={totalTip} onChange={(e) => setTotalTip(e.target.value)}
                    />
                    <TextField
                      select label="Currency" value={currency} onChange={(e) => setCurrency(e.target.value)} sx={{ minWidth: 120 }}
                    >
                      <MenuItem value="USD">USD</MenuItem>
                      <MenuItem value="EUR">EUR</MenuItem>
                      <MenuItem value="INR">INR</MenuItem>
                      <MenuItem value="GBP">GBP</MenuItem>
                    </TextField>
                  </Box>

                  <TextField
                    fullWidth label="Restaurant Name"
                    value={restaurantName} onChange={(e) => setRestaurantName(e.target.value)}
                  />

                  <TextField
                    select fullWidth label="Distribution Type"
                    value={distributionType} onChange={(e) => setDistributionType(e.target.value)}
                  >
                    <MenuItem value="EQUAL">Equal Split</MenuItem>
                    <MenuItem value="PERCENTAGE">Percentage Split</MenuItem>
                  </TextField>

                  <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)' }} />
                  
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="h6">Team Members</Typography>
                    <Button onClick={handleAddMember} disabled={members.length >= 20}>+ Add Member</Button>
                  </Box>

                  {members.map((member, idx) => (
                    <Box key={idx} sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                      <TextField
                        fullWidth label={`Member ${idx + 1} Name`}
                        value={member.name}
                        onChange={(e) => handleMemberChange(idx, 'name', e.target.value)}
                      />
                      {distributionType === 'PERCENTAGE' && (
                        <TextField
                          label="%" type="number" sx={{ width: 120 }}
                          value={member.allocationPercentage}
                          onChange={(e) => handleMemberChange(idx, 'allocationPercentage', e.target.value)}
                        />
                      )}
                      <IconButton 
                        color="error" 
                        onClick={() => handleRemoveMember(idx)}
                        disabled={members.length <= 2}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Box>
                  ))}

                  <Button 
                    variant="contained" 
                    size="large" 
                    onClick={handlePreview}
                    disabled={loading}
                    sx={{ mt: 2 }}
                  >
                    Preview & Calculate
                  </Button>
                </Stack>
              </CardContent>
            </Card>
          )}
        </Grid>

        <Grid item xs={12} md={5}>
          <Typography variant="h5" sx={{ mb: 3 }}>Pool History</Typography>
          <Stack spacing={2}>
            {pools.length === 0 ? (
              <Typography sx={{ color: '#9AA0A6' }}>No tip pools found.</Typography>
            ) : (
              pools.map((pool) => (
                <Card 
                  key={pool.id} 
                  sx={{ 
                    cursor: 'pointer',
                    '&:hover': { borderColor: 'rgba(255,255,255,0.2)' }
                  }}
                  onClick={() => { setViewPool(pool); setPreviewPool(null); setError(''); }}
                >
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="h6">{pool.restaurantName}</Typography>
                      <Typography variant="h6" color="primary">{pool.currency} {pool.totalTip.toFixed(2)}</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                      <Chip size="small" label={pool.status} color={pool.status === 'FINALIZED' ? 'success' : 'warning'} />
                      <Typography variant="body2" sx={{ color: '#9AA0A6' }}>{pool.members.length} members</Typography>
                    </Box>
                  </CardContent>
                </Card>
              ))
            )}
          </Stack>
        </Grid>
      </Grid>
    </Box>
  );
};

export default TipPoolPage;

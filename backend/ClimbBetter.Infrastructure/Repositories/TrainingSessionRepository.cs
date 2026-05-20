using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Domain.Training;
using ClimbBetter.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace ClimbBetter.Infrastructure.Repositories;

public class TrainingSessionRepository : ITrainingSessionRepository
{
    private readonly ClimbBetterDbContext _dbContext;

    public TrainingSessionRepository(ClimbBetterDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task AddAsync(
        TrainingSession trainingSession,
        CancellationToken cancellationToken)
    {
        await _dbContext.TrainingSessions.AddAsync(trainingSession, cancellationToken);
        await _dbContext.SaveChangesAsync(cancellationToken);
    }

    public async Task<List<TrainingSession>> GetAllAsync(CancellationToken cancellationToken)
    {
        return await _dbContext.TrainingSessions
            .Include(x => x.Location)
            .Include(x => x.Entries)
            .OrderByDescending(x => x.Date)
            .ToListAsync(cancellationToken);
    }

    public async Task<bool> ExistsAsync(
        Guid id,
        CancellationToken cancellationToken)
    {
        return await _dbContext.TrainingSessions
            .AnyAsync(s => s.Id == id, cancellationToken);
    }

    public async Task AddEntryAsync(
        TrainingEntry trainingEntry,
        CancellationToken cancellationToken)
    {
        await _dbContext.TrainingEntries.AddAsync(trainingEntry, cancellationToken);
        await _dbContext.SaveChangesAsync(cancellationToken);
    }
}
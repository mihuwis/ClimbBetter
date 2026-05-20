using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Domain.Training;
using ClimbBetter.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace ClimbBetter.Infrastructure.Repositories;


public class DifficultyRepository : IDifficultyRepository
{
    private readonly ClimbBetterDbContext _dbContext;

    public DifficultyRepository(ClimbBetterDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task<List<Difficulty>> GetAllAsync(CancellationToken cancellationToken)
    {
        return await _dbContext.Difficulties
            .OrderBy(d => d.Points)
            .ToListAsync(cancellationToken);
    }

    public async Task<Difficulty?> GetByIdAsync(
    int id,
    CancellationToken cancellationToken)
{
    return await _dbContext.Difficulties
        .FirstOrDefaultAsync(d => d.Id == id, cancellationToken);
}
}